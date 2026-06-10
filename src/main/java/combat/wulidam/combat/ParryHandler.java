package combat.wulidam.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles parry resolution — both successful and failed parries.
 */
public class ParryHandler {

    private static final int PARRY_SUCCESS_REWARD_TICKS = 10;

    public static void resolveSuccessfulParry(LivingEntity attacker,
                                              ServerPlayerEntity defender,
                                              WeaponData weaponData) {
        CombatStateManager.applyStun(attacker, weaponData.parryStunTicks());
        applyParryKnockback(attacker, defender, weaponData.parryKnockback());
        CombatStateManager.applyParrySuccess(defender, PARRY_SUCCESS_REWARD_TICKS);
    }

    private static void applyParryKnockback(LivingEntity attacker,
                                            ServerPlayerEntity defender,
                                            float strength) {
        double dx = attacker.getX() - defender.getX();
        double dz = attacker.getZ() - defender.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance > 0.01) {
            double knockbackX = (dx / distance) * strength;
            double knockbackZ = (dz / distance) * strength;
            attacker.setVelocity(attacker.getVelocity().add(knockbackX, 0.1, knockbackZ));
            attacker.velocityDirty = true;
        }
    }
}
