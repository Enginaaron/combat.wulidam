package combat.wulidam.mixin;

import combat.wulidam.combat.*;
import combat.wulidam.item.ShieldItem;
import combat.wulidam.item.SoulsWeaponItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into LivingEntity.damage() to implement dodge invincibility frames and parries.
 * When a player is in the DODGING state, all incoming damage is cancelled.
 * when a player is in the PARRYING state, incoming attacks from the front are parried.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void soulslikecombat$handleCombatDefenses(ServerWorld world, DamageSource source,
                                                     float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof ServerPlayerEntity player) {
            PlayerCombatData data = CombatStateManager.get(player.getUuid());
            if (data == null) return;

            // 1. i-frames (Dodge)
            if (data.getCurrentState().hasIFrames()) {
                PostureHandler.onDodge(player, data);
                CombatStateManager.syncStateToClient(player, data);
                cir.setReturnValue(false);
                return;
            }

            // Get attacker entity from the damage source
            LivingEntity attacker = null;
            if (source.getAttacker() instanceof LivingEntity le) {
                attacker = le;
            }

            if (attacker != null) {
                // 2. Parry check
                if (data.getCurrentState() == CombatState.PARRYING) {
                    Vec3d toAttacker = new Vec3d(attacker.getX() - player.getX(), attacker.getY() - player.getY(), attacker.getZ() - player.getZ()).normalize();
                    Vec3d playerLook = player.getRotationVec(1.0f).normalize();
                    if (playerLook.dotProduct(toAttacker) > 0.7071) {
                        WeaponData weaponData = WeaponRegistry.getWeaponData(data.getCurrentWeaponDataId());
                        if (weaponData != null) {
                            ParryHandler.resolveSuccessfulParry(attacker, player, weaponData);
                            PostureHandler.onParry(player, data);

                            if (attacker instanceof ServerPlayerEntity attackerPlayer) {
                                PlayerCombatData attackerData = CombatStateManager.get(attackerPlayer.getUuid());
                                if (attackerData != null) {
                                    PostureHandler.onParried(attackerPlayer, attackerData);
                                    CombatStateManager.syncStateToClient(attackerPlayer, attackerData);
                                }
                            }
                            
                            CombatStateManager.syncStateToClient(player, data);
                            cir.setReturnValue(false);
                            return;
                        }
                    }
                }

                // 3. Shield block — only if actively using the shield in off-hand and facing attacker
                if (player.isUsingItem() && player.getActiveItem().getItem() instanceof ShieldItem && player.getActiveHand() == Hand.OFF_HAND) {
                    if (data.getShieldCooldownRemaining() > 0) return;

                    Vec3d toAttacker = new Vec3d(attacker.getX() - player.getX(), attacker.getY() - player.getY(), attacker.getZ() - player.getZ()).normalize();
                    Vec3d playerLook = player.getRotationVec(1.0f).normalize();
                    if (playerLook.dotProduct(toAttacker) > 0.7071) {
                        float staminaCost = amount;
                        if (data.getStamina() < staminaCost) {
                            data.setStamina(0);
                            data.setShieldCooldownRemaining(200); // 10s
                            player.stopUsingItem();
                        } else {
                            data.setStamina(data.getStamina() - staminaCost);
                        }
                        CombatStateManager.syncStateToClient(player, data);
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }

            // 4. posture loss and hemorrhage
            PostureHandler.onHit(player, data, amount);

            data.addDamageRecord(amount);
            if (data.getRecentDamageTaken() > player.getMaxHealth() * 0.3f) {
                float trueDamage = player.getMaxHealth() * 0.1f;
                player.setHealth(player.getHealth() - trueDamage);
                data.resetHemorrhage();
            }

            CombatStateManager.syncStateToClient(player, data);
        }
    }

    @ModifyVariable(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private float soulslikecombat$applyStunDamageBonus(float amount, ServerWorld world, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity player) {
            PlayerCombatData data = CombatStateManager.get(player.getUuid());
            if (data != null && data.getCurrentState() == CombatState.STUNNED) {
                return amount * 1.25f;
            }
        }
        return amount;
    }
}
