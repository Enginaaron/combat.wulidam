package combat.wulidam.combat;

import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.item.SoulsWeaponItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative combat state machine manager.
 * Maintains a PlayerCombatData instance for every player engaged in souls-like combat.
 * All state transitions and validations happen here.
 */
public class CombatStateManager {
    private static final Map<UUID, PlayerCombatData> PLAYER_DATA = new HashMap<>();

    // --- Data Access ---

    public static PlayerCombatData getOrCreate(ServerPlayerEntity player) {
        return PLAYER_DATA.computeIfAbsent(player.getUuid(), PlayerCombatData::new);
    }

    public static PlayerCombatData get(UUID uuid) {
        return PLAYER_DATA.get(uuid);
    }

    public static void remove(UUID uuid) {
        PLAYER_DATA.remove(uuid);
    }

    // --- Tick ---

    /**
     * Tick the combat state machine for a player. Called every server tick.
     */
    public static void tick(ServerPlayerEntity player) {
        PlayerCombatData data = getOrCreate(player);

        // Update the cached weapon reference
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.getItem() instanceof SoulsWeaponItem weapon) {
            data.setCurrentWeaponDataId(weapon.getWeaponDataId());
        } else {
            if (data.getCurrentState() != CombatState.IDLE) {
                data.reset();
            }
            return;
        }

        WeaponData weaponData = WeaponRegistry.getWeaponData(data.getCurrentWeaponDataId());
        if (weaponData == null) return;

        data.tickTimers();

        if (data.getStateTicksRemaining() <= 0) {
            handleStateExpiry(player, data, weaponData);
        }
    }

    private static void handleStateExpiry(ServerPlayerEntity player, PlayerCombatData data, WeaponData weaponData) {
        ServerWorld world = player.getEntityWorld();
        long currentTick = world.getTime();

        switch (data.getCurrentState()) {
            case WIND_UP -> {
                int activeTicks = weaponData.getActiveForCombo(data.getComboIndex());
                data.setState(CombatState.ATTACKING, activeTicks);
            }
            case ATTACKING -> {
                data.setState(CombatState.RECOVERY, weaponData.recoveryTicks());
            }
            case RECOVERY -> {
                data.setLastRecoveryEndTick(currentTick);
                data.setState(CombatState.IDLE, 0);
            }
            case PARRYING -> {
                data.setState(CombatState.PARRY_FAIL, weaponData.parryFailStunTicks());
                data.setParryCooldownRemaining(weaponData.parryCooldownTicks());
            }
            case PARRY_SUCCESS -> {
                data.setState(CombatState.IDLE, 0);
                data.setComboIndex(0);
            }
            case PARRY_FAIL -> {
                data.setState(CombatState.IDLE, 0);
                data.setComboIndex(0);
            }
            case DODGING -> {
                data.setState(CombatState.IDLE, 0);
                data.setDodgeCooldownRemaining(weaponData.dodgeCooldownTicks());
                data.setComboIndex(0);
            }
            case STUNNED -> {
                data.setState(CombatState.IDLE, 0);
                data.setComboIndex(0);
            }
            case IDLE -> {
                if (data.getComboIndex() > 0) {
                    long ticksSinceRecovery = currentTick - data.getLastRecoveryEndTick();
                    if (ticksSinceRecovery > weaponData.comboWindowTicks()) {
                        data.setComboIndex(0);
                    }
                }
            }
        }
    }

    // --- Action Requests ---

    public static boolean requestAttack(ServerPlayerEntity player) {
        PlayerCombatData data = getOrCreate(player);
        WeaponData weaponData = WeaponRegistry.getWeaponData(data.getCurrentWeaponDataId());
        if (weaponData == null) return false;

        if (!data.getCurrentState().canAct()) return false;

        ServerWorld world = player.getEntityWorld();
        long currentTick = world.getTime();

        if (data.getComboIndex() > 0) {
            long ticksSinceRecovery = currentTick - data.getLastRecoveryEndTick();
            if (ticksSinceRecovery > weaponData.comboWindowTicks()) {
                data.setComboIndex(0);
            }
        }

        if (data.getComboIndex() >= weaponData.comboCount()) {
            data.setComboIndex(0);
        }

        int windUp = weaponData.getWindUpForCombo(data.getComboIndex());
        data.setState(CombatState.WIND_UP, windUp);

        SoulsLikeCombat.LOGGER.debug("Player {} → WIND_UP (combo {}/{})",
                player.getName().getString(), data.getComboIndex() + 1, weaponData.comboCount());

        return true;
    }

    public static boolean requestParry(ServerPlayerEntity player) {
        PlayerCombatData data = getOrCreate(player);
        WeaponData weaponData = WeaponRegistry.getWeaponData(data.getCurrentWeaponDataId());
        if (weaponData == null) return false;

        if (!data.getCurrentState().canAct()) return false;
        if (data.getParryCooldownRemaining() > 0) return false;

        data.setState(CombatState.PARRYING, weaponData.parryWindowTicks());
        data.setComboIndex(0);
        return true;
    }

    public static boolean requestDodge(ServerPlayerEntity player) {
        PlayerCombatData data = getOrCreate(player);
        WeaponData weaponData = WeaponRegistry.getWeaponData(data.getCurrentWeaponDataId());
        if (weaponData == null) return false;

        CombatState state = data.getCurrentState();
        if (state != CombatState.IDLE && state != CombatState.PARRY_SUCCESS
                && state != CombatState.RECOVERY) {
            return false;
        }
        if (data.getDodgeCooldownRemaining() > 0) return false;

        data.setState(CombatState.DODGING, weaponData.dodgeTicks());
        data.setComboIndex(0);
        return true;
    }

    public static void applyStun(ServerPlayerEntity player, int stunTicks) {
        PlayerCombatData data = getOrCreate(player);
        data.setState(CombatState.STUNNED, stunTicks);
        data.setComboIndex(0);
    }

    public static void applyParrySuccess(ServerPlayerEntity player, int rewardTicks) {
        PlayerCombatData data = getOrCreate(player);
        data.setState(CombatState.PARRY_SUCCESS, rewardTicks);
        data.setParryCooldownRemaining(0);
    }

    public static void advanceCombo(ServerPlayerEntity player) {
        PlayerCombatData data = getOrCreate(player);
        data.setComboIndex(data.getComboIndex() + 1);
    }
}
