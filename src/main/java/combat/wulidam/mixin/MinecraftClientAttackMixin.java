package combat.wulidam.mixin;

import combat.wulidam.SoulsLikeCombatClient;
import combat.wulidam.item.SoulsWeaponItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
// stops client clicks when the custom combat state says no
public class MinecraftClientAttackMixin {

    // stop click before vanilla sends attack packet/swing when souls state says "not ready"
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void soulslikecombat$blockClientAttackIfNotReady(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        ItemStack mainHand = player.getMainHandStack();
        if (!(mainHand.getItem() instanceof SoulsWeaponItem)) {
            return;
        }

        if (!SoulsLikeCombatClient.getCurrentCombatState().canAct()) {
            cir.setReturnValue(false);
        }
    }
}
