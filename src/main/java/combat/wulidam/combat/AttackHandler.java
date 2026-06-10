package combat.wulidam.combat;

import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.item.ShieldItem;
import combat.wulidam.network.s2c.HitResultS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.Hand;
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
                sendHitResult(attacker, target, 0, HitResultS2CPayload.HIT_PARRIED);
            }
            case DODGING -> {
                // i-frames — miss
                sendHitResult(attacker, target, 0, HitResultS2CPayload.HIT_DODGED);
            }
            case WIND_UP, ATTACKING -> {
                if (target.isUsingItem() && target.getActiveItem().getItem() instanceof ShieldItem shield && target.getActiveHand() == Hand.OFF_HAND) {
                    // Only block if target is actively using the shield in the off-hand and is facing attacker
                    if (isFacingSameDirection(attacker, target)) {
                        sendHitResult(attacker, target, 0, HitResultS2CPayload.HIT_BLOCKED);
                        return;
                    }
                }

                float damage = weaponData.getDamageForCombo(attackerData.getComboIndex());
                DamageCalculator.applyDamage(attacker, target, damage, weaponData);
                CombatStateManager.applyStun(target, weaponData.interruptStunTicks());
                sendHitResult(attacker, target, damage, HitResultS2CPayload.HIT_INTERRUPTED);
            }
            default -> {
                float damage = weaponData.getDamageForCombo(attackerData.getComboIndex());
                DamageCalculator.applyDamage(attacker, target, damage, weaponData);
                CombatStateManager.applyStun(target, weaponData.hitStunTicks());
                sendHitResult(attacker, target, damage, HitResultS2CPayload.HIT_NORMAL);
            }
        }
    }

    private static void handlePvEHit(ServerPlayerEntity attacker, LivingEntity target,
                                     PlayerCombatData attackerData, WeaponData weaponData) {
        // --- SHIELD BLOCK CHECK ---
        if (target instanceof ServerPlayerEntity player) {
            if (player.isUsingItem() && player.getActiveItem().getItem() instanceof ShieldItem && player.getActiveHand() == Hand.OFF_HAND) {
                // Only block if player is actively using the shield in the off-hand and is facing attacker
                if (isFacingSameDirection(attacker, player)) {
                    sendHitResult(attacker, player, 0, HitResultS2CPayload.HIT_BLOCKED);
                    return;
                }
            }
        }

        float damage = weaponData.getDamageForCombo(attackerData.getComboIndex());
        DamageCalculator.applyDamage(attacker, target, damage, weaponData);
    }

    private static boolean isFacingSameDirection(ServerPlayerEntity attacker, LivingEntity target) {
        // Check if the target (defender) is looking towards the attacker using eye positions.
        Vec3d toAttacker = attacker.getEyePos().subtract(target.getEyePos()).normalize();
        Vec3d targetLook = target.getRotationVec(1.0f).normalize();
        // dot > 0.7071 means defender is facing within ~45° of the attacker
        return targetLook.dotProduct(toAttacker) > 0.7071;
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

    /**
     * Send hit result feedback to both the attacker and the target (if target is a player).
     */
    private static void sendHitResult(ServerPlayerEntity attacker, ServerPlayerEntity target,
                                      float damage, int hitType) {
        HitResultS2CPayload payload = new HitResultS2CPayload(
                attacker.getUuid(), target.getUuid(), damage, hitType);
        ServerPlayNetworking.send(attacker, payload);
        ServerPlayNetworking.send(target, payload);
    }
}
