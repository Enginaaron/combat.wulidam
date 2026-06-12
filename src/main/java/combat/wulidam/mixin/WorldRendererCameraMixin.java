package combat.wulidam.mixin;

import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.client.CameraController;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Apply camera offsets by manipulating the model-view stack at a point where the world is about to be rendered.
 * Uses the runtime WorldRenderer.render signature present in this mapping.
 */
@Mixin(WorldRenderer.class)
public class WorldRendererCameraMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void soulslikecombat$onWorldRenderHead(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean someFlag, Camera camera, Matrix4f m1, Matrix4f m2, Matrix4f m3, GpuBufferSlice gpu, Vector4f vec, boolean anotherFlag, CallbackInfo ci) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;

            // drive camera tick with 0 for now
            CameraController.tick(client, 0.0f);

            boolean third = false;
            try {
                third = CameraController.isThirdPerson(client);
            } catch (Throwable ignored) {}
            if (!third) return;

            Vec3d off = CameraController.getPositionOffset();

            try {
                Object mv = RenderSystem.getModelViewStack();
                if (mv != null) {
                    try {
                        java.lang.reflect.Method push = mv.getClass().getMethod("push");
                        push.invoke(mv);
                    } catch (NoSuchMethodException ignored) {}

                    try {
                        java.lang.reflect.Method translate = mv.getClass().getMethod("translate", double.class, double.class, double.class);
                        translate.invoke(mv, off.x, off.y, off.z);
                    } catch (NoSuchMethodException ignored) {}

                    try {
                        java.lang.reflect.Method apply = RenderSystem.class.getMethod("applyModelViewMatrix");
                        apply.invoke(null);
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Throwable t) {
                SoulsLikeCombat.LOGGER.debug("WorldRendererCameraMixin modelview manipulation failed", t);
            }

            SoulsLikeCombat.LOGGER.info("WorldRendererCameraMixin applied offset {} {} {}", off.x, off.y, off.z);
        } catch (Throwable t) {
            SoulsLikeCombat.LOGGER.debug("WorldRendererCameraMixin failed", t);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void soulslikecombat$onWorldRenderReturn(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean someFlag, Camera camera, Matrix4f m1, Matrix4f m2, Matrix4f m3, GpuBufferSlice gpu, Vector4f vec, boolean anotherFlag, CallbackInfo ci) {
        try {
            try {
                Object mv = RenderSystem.getModelViewStack();
                if (mv != null) {
                    try {
                        java.lang.reflect.Method pop = mv.getClass().getMethod("pop");
                        pop.invoke(mv);
                    } catch (NoSuchMethodException ignored) {}

                    try {
                        java.lang.reflect.Method apply = RenderSystem.class.getMethod("applyModelViewMatrix");
                        apply.invoke(null);
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Throwable t) {
                // ignore
            }
        } catch (Throwable t) {
            // ignore
        }
    }
}
