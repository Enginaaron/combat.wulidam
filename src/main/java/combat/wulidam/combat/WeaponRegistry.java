package combat.wulidam.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.network.s2c.WeaponDataSyncS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.util.*;

/**
 * Loads and stores WeaponData from JSON files in the datapack directory.
 * Weapon data files are located at: data/soulslikecombat/weapon_data/<weapon_id>.json
 *
 * This is a server-side registry. Data is synced to clients via
 * WeaponDataSyncS2CPayload in Phase 2.
 */
public class WeaponRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_PATH = "weapon_data";

    /** All loaded weapon data, keyed by weapon identifier. */
    private static final Map<Identifier, WeaponData> WEAPON_DATA = new HashMap<>();

    /**
     * Register the resource reload listener. Called once from ModInitializer.
     */
    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.of(SoulsLikeCombat.MOD_ID, DATA_PATH);
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        loadWeaponData(manager);
                    }
                }
        );
    }

    /**
     * Load all weapon data JSONs from the data directory.
     */
    private static void loadWeaponData(ResourceManager manager) {
        WEAPON_DATA.clear();

        var resources = manager.findResources(DATA_PATH,
                id -> id.getNamespace().equals(SoulsLikeCombat.MOD_ID) && id.getPath().endsWith(".json"));

        for (var entry : resources.entrySet()) {
            Identifier fileId = entry.getKey();
            try (var reader = new InputStreamReader(entry.getValue().getInputStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);

                // Derive weapon ID from file path: "weapon_data/longsword.json" -> "soulslikecombat:longsword"
                String path = fileId.getPath();
                String weaponName = path.substring(DATA_PATH.length() + 1, path.length() - 5); // strip prefix and .json
                Identifier weaponId = Identifier.of(fileId.getNamespace(), weaponName);

                WeaponData data = parseWeaponData(weaponId, json);
                WEAPON_DATA.put(weaponId, data);

                SoulsLikeCombat.LOGGER.info("Loaded weapon data: {}", weaponId);
            } catch (Exception e) {
                SoulsLikeCombat.LOGGER.error("Failed to load weapon data from {}", fileId, e);
            }
        }

        SoulsLikeCombat.LOGGER.info("Loaded {} weapon data entries", WEAPON_DATA.size());
    }

    /**
     * Parse a JSON object into a WeaponData record.
     */
    private static WeaponData parseWeaponData(Identifier id, JsonObject json) {
        List<ComboAttack> comboAttacks = new ArrayList<>();
        if (json.has("comboAttacks")) {
            JsonArray arr = json.getAsJsonArray("comboAttacks");
            for (var element : arr) {
                JsonObject ca = element.getAsJsonObject();
                comboAttacks.add(new ComboAttack(
                        getFloat(ca, "damageMultiplier", 1.0f),
                        getFloat(ca, "rangeMultiplier", 1.0f),
                        getInt(ca, "windUpTicks", -1),
                        getInt(ca, "activeTicks", -1)
                ));
            }
        }

        return new WeaponData(
                id,
                getFloat(json, "baseDamage", 5.0f),
                getFloat(json, "attackRange", 3.0f),
                getInt(json, "windUpTicks", 5),
                getInt(json, "activeTicks", 3),
                getInt(json, "recoveryTicks", 10),
                getInt(json, "parryWindowTicks", 6),
                getInt(json, "parryStunTicks", 15),
                getFloat(json, "parryKnockback", 1.0f),
                getInt(json, "parryFailStunTicks", 10),
                getInt(json, "parryCooldownTicks", 30),
                getInt(json, "dodgeTicks", 10),
                getInt(json, "dodgeIFrames", 7),
                getFloat(json, "dodgeDistance", 3.0f),
                getInt(json, "dodgeCooldownTicks", 25),
                getInt(json, "hitStunTicks", 5),
                getInt(json, "interruptStunTicks", 10),
                getInt(json, "comboCount", 3),
                getInt(json, "comboWindowTicks", 15),
                comboAttacks
        );
    }

    // --- JSON helpers ---

    private static float getFloat(JsonObject json, String key, float defaultValue) {
        return json.has(key) ? json.get(key).getAsFloat() : defaultValue;
    }

    private static int getInt(JsonObject json, String key, int defaultValue) {
        return json.has(key) ? json.get(key).getAsInt() : defaultValue;
    }

    // --- Public API ---

    /**
     * Get weapon data by identifier. Returns null if not found.
     */
    public static WeaponData getWeaponData(Identifier id) {
        return WEAPON_DATA.get(id);
    }

    /**
     * @return an unmodifiable view of all loaded weapon data.
     */
    public static Collection<WeaponData> getAllWeaponData() {
        return Collections.unmodifiableCollection(WEAPON_DATA.values());
    }

    /**
     * @return true if weapon data exists for the given identifier.
     */
    public static boolean hasWeaponData(Identifier id) {
        return WEAPON_DATA.containsKey(id);
    }

    // --- Client-Side Data ---

    /**
     * Called by the client S2C receiver to populate the client-side weapon data cache.
     * On the client, weapon data is received from the server via WeaponDataSyncS2CPayload
     * rather than loaded from JSON files directly.
     */
    public static void registerClientWeaponData(WeaponData data) {
        WEAPON_DATA.put(data.id(), data);
    }

    // --- Server-Side Sync ---

    /**
     * Send all loaded weapon data to a player. Called when a player joins the server.
     */
    public static void syncToPlayer(ServerPlayerEntity player) {
        List<WeaponDataSyncS2CPayload.WeaponDataEntry> entries = new ArrayList<>();
        for (WeaponData data : WEAPON_DATA.values()) {
            entries.add(WeaponDataSyncS2CPayload.fromWeaponData(data));
        }
        ServerPlayNetworking.send(player, new WeaponDataSyncS2CPayload(entries));
        SoulsLikeCombat.LOGGER.debug("Synced {} weapon data entries to {}",
                entries.size(), player.getName().getString());
    }
}
