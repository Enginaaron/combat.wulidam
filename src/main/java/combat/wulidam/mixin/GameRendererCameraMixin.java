package combat.wulidam.mixin;

import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.client.CameraController;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Apply camera offsets computed by CameraController. This mixin targets the GameRenderer.render
 * method signature used in 1.21.11: (RenderTickCounter, boolean). The injected methods accept
 * RenderTickCounter and a boolean to match the runtime descriptor.
 */
@Mixin(GameRenderer.class)

public class GameRendererCameraMixin {
    @Shadow @Final private MinecraftClient client;

    // store original camera position/rotation so it can be restored after rendering
    private static final Map<Object, Object[]> ORIGINAL_CAMERA = new WeakHashMap<>();

    @Inject(method = "render", at = @At("HEAD"))
    private void soulslikecombat$applyCameraOffsets(net.minecraft.client.render.RenderTickCounter tickCounter, boolean something, CallbackInfo ci) {
        try {
            if (client == null) return;

            // compute render tick delta reflectively (mapping-independent)
            float tickDelta = 0.0f;
            try {
                java.lang.reflect.Method m = tickCounter.getClass().getMethod("getTickDelta");
                Object v = m.invoke(tickCounter);
                if (v instanceof Number) tickDelta = ((Number)v).floatValue();
            } catch (Throwable ignored) {
                try {
                    java.lang.reflect.Method m2 = tickCounter.getClass().getMethod("tickDelta");
                    Object v2 = m2.invoke(tickCounter);
                    if (v2 instanceof Number) tickDelta = ((Number)v2).floatValue();
                } catch (Throwable ignored2) {}
            }
            CameraController.tick(client, tickDelta);

            try {
                if (!CameraController.isThirdPerson(client)) return;
            } catch (Throwable ignored1) {}

            Vec3d off = CameraController.getPositionOffset();
            SoulsLikeCombat.LOGGER.info("GameRendererCameraMixin applying offset {} {} {}", off.x, off.y, off.z);

            // Reflective camera repositioning disabled in GameRenderer mixin.
            // WorldRendererCameraMixin performs the modelview push/translate/pop to shift the rendered world
            // so the player's view is naturally offset without modifying the engine Camera object.

        } catch (Throwable t) {
            SoulsLikeCombat.LOGGER.debug("CameraMixin failed to apply offsets", t);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void soulslikecombat$onRenderReturn(net.minecraft.client.render.RenderTickCounter tickCounter, boolean something, CallbackInfo ci) {
        try {
            try {
                Object mv = RenderSystem.getModelViewStack();
                if (mv != null) {
                    try {
                        java.lang.reflect.Method pop = mv.getClass().getMethod("pop");
                        pop.invoke(mv);
                    } catch (NoSuchMethodException ignored14) {}

                    try {
                        java.lang.reflect.Method apply = RenderSystem.class.getMethod("applyModelViewMatrix");
                        apply.invoke(null);
                    } catch (NoSuchMethodException ignored15) {}
                }
            } catch (Throwable t) {
                // ignore
            }

            // No reflective camera restoration necessary; WorldRenderer handles modelview stack restoration.
        } catch (Throwable t) {
            // ignore
        }
    }
}

