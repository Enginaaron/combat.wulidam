package combat.wulidam.combat;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Handles attack resolution during the ATTACKING state's active damage frames.
 * Performs server-side hit detection via area sweep, checks target combat state,
 * and delegates to DamageCalculator.
 */
public class AttackHandler {

    public static void processAttackTick(ServerPlayerEntity attacker) {
        PlayerCombatData attackerData = CombatStateManager.getOrCreate(attacker);
        if (attackerData.getCurrentState() != CombatState.ATTACKING) return;
        if (attackerData.hasAttackHitConnected()) return;

        WeaponData weaponData = WeaponRegistry.getWeaponData(attackerData.getCurrentWeaponDataId());
        if (weaponData == null) return;

        float range = weaponData.getRangeForCombo(attackerData.getComboIndex());
        List<Entity> targets = findTargetsInRange(attacker, range);

        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity target)) continue;
            if (target == attacker) continue;
            if (!target.isAlive()) continue;

            if (target instanceof ServerPlayerEntity targetPlayer) {
                handlePvPHit(attacker, targetPlayer, attackerData, weaponData);
            } else {
                handlePvEHit(attacker, target, attackerData, weaponData);
            }

            attackerData.setAttackHitConnected(true);
            CombatStateManager.advanceCombo(attacker);
            break;
        }
    }

    private static void handlePvPHit(ServerPlayerEntity attacker, ServerPlayerEntity target,
                                     PlayerCombatData attackerData, WeaponData weaponData) {
        PlayerCombatData targetData = CombatStateManager.getOrCreate(target);

        switch (targetData.getCurrentState()) {
            case PARRYING -> {
                ParryHandler.resolveSuccessfulParry(attacker, target, weaponData);
            }
            case DODGING -> {
                // i-frames — miss
            }
            case WIND_UP, ATTACKING -> {
                float damage = weaponData.getDamageForCombo(attackerData.getComboIndex());
                DamageCalculator.applyDamage(attacker, target, damage, weaponData);
                CombatStateManager.applyStun(target, weaponData.interruptStunTicks());
            }
            default -> {
                float damage = weaponData.getDamageForCombo(attackerData.getComboIndex());
                DamageCalculator.applyDamage(attacker, target, damage, weaponData);
                CombatStateManager.applyStun(target, weaponData.hitStunTicks());
            }
        }
    }

    private static void handlePvEHit(ServerPlayerEntity attacker, LivingEntity target,
                                     PlayerCombatData attackerData, WeaponData weaponData) {
        float damage = weaponData.getDamageForCombo(attackerData.getComboIndex());
        DamageCalculator.applyDamage(attacker, target, damage, weaponData);
    }

    private static List<Entity> findTargetsInRange(ServerPlayerEntity attacker, float range) {
        Vec3d eyePos = attacker.getEyePos();
        Vec3d lookDir = attacker.getRotationVec(1.0f);
        Vec3d reachEnd = eyePos.add(lookDir.multiply(range));

        Box searchBox = new Box(eyePos, reachEnd).expand(1.0);

        ServerWorld world = attacker.getEntityWorld();
        return world.getOtherEntities(attacker, searchBox,
                entity -> entity instanceof LivingEntity && entity.isAlive()
                        && entity.squaredDistanceTo(attacker) <= range * range);
    }
}
