package combat.wulidam.item;

import combat.wulidam.SoulsLikeCombat;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/**
 * Registers all souls-like combat weapon items and adds them to a creative tab called "Combat"
 */
// registers every custom weapon item for the mod
public class ModItems {

    public static final Item HEAVY_SWORD = register("heavysword",
            HeavyswordItem::new,
            new Item.Settings().sword(ToolMaterial.IRON, 15, -3.7f));

    public static final Item SWORD = register("sword",
            SwordItem::new,
            new Item.Settings().sword(ToolMaterial.IRON, 8, -2.4f));

    public static final Item SHIELD = register("shield",
            ShieldItem::new,
            new Item.Settings().sword(ToolMaterial.IRON, 3, -2.4f));

    /**
     * Sword and Shield: Medium speed, medium damage, easier parry.
     */
    public static final Item SWORD_AND_SHIELD = register("sword_and_shield",
            SwordAndShieldItem::new,
            new Item.Settings().sword(ToolMaterial.IRON, 3, -2.4f));

    /**
     * Dagger: Fast speed, low damage, very short range.
     */
    public static final Item DAGGER = register("dagger",
            DaggerItem::new,
            new Item.Settings().sword(ToolMaterial.IRON, 3, -1.5f));

    /**
     * Scythe: Slow speed, high damage, long range.
     */
    public static final Item SCYTHE = register("scythe",
            ScytheItem::new,
            new Item.Settings().sword(ToolMaterial.IRON, 6, -3.0f));

    /**
     * Longsword: Very slow, very high damage, heavy lag.
     */
    public static final Item LONGSWORD = register("longsword",
            LongswordItem::new,
            new Item.Settings().sword(ToolMaterial.IRON, 8, -3.2f));

    /**
     * Greatbow: Slower draw, higher arrow velocity. Uses normal arrows.
     */
    public static final Item GREATBOW = register("greatbow",
            GreatbowItem::new,
            new Item.Settings().maxDamage(512));

    // --- Registration ---

    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM,
                Identifier.of(SoulsLikeCombat.MOD_ID, name));
        return Items.register(key, factory, settings);
    }

    /**
     * Initialize all items and add them to the Combat creative tab.
     * Must be called from the main ModInitializer.
     */
    public static void initialize() {
        SoulsLikeCombat.LOGGER.info("Registering SoulsLikeCombat items...");

        // Add all weapons to the Combat creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(SWORD_AND_SHIELD);
            entries.add(DAGGER);
            entries.add(SCYTHE);
            entries.add(LONGSWORD);
            entries.add(GREATBOW);
            entries.add(SWORD);
            entries.add(SHIELD);
            entries.add(HEAVY_SWORD);
        });
    }
}
