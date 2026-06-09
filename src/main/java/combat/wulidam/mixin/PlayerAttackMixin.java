package combat.wulidam.mixin;

import combat.wulidam.combat.CombatStateManager;
import combat.wulidam.item.SoulsWeaponItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables vanilla attack mechanics when the player is holding a
 * SoulsWeaponItem
 *
 * This prevents vanilla damage calculation, sweep attacks, and cooldown logic
 * from interfering with the souls-like combat system.
 */
@Mixin(PlayerEntity.class)
public class PlayerAttackMixin {

    @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void soulslikecombat$cancelVanillaAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        ItemStack mainHand = player.getMainHandStack();

        if (mainHand.getItem() instanceof SoulsWeaponItem) {
            ci.cancel();

            // On the server, trigger the custom combat state machine.
            if (player instanceof ServerPlayerEntity serverPlayer) {
                CombatStateManager.requestAttack(serverPlayer);
            }
        }
    }
}
