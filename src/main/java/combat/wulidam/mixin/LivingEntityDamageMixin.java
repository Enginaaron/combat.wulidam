package combat.wulidam.mixin;

import combat.wulidam.combat.CombatState;
import combat.wulidam.combat.CombatStateManager;
import combat.wulidam.combat.PlayerCombatData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into LivingEntity.damage() to implement dodge invincibility frames.
 * When a player is in the DODGING state, all incoming damage is cancelled.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void soulslikecombat$cancelDamageOnDodge(ServerWorld world, DamageSource source,
                                                     float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof ServerPlayerEntity player) {
            PlayerCombatData data = CombatStateManager.get(player.getUuid());
            if (data != null && data.getCurrentState().hasIFrames()) {
                // Player is dodging with active i-frames — cancel all damage
                cir.setReturnValue(false);
            }
        }
    }
}
