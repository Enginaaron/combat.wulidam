package combat.wulidam;

import combat.wulidam.client.StaminaHudRenderer;
import combat.wulidam.combat.CombatState;
import combat.wulidam.combat.WeaponData;
import combat.wulidam.combat.WeaponRegistry;
import combat.wulidam.network.s2c.CombatStateS2CPayload;
import combat.wulidam.network.s2c.HitResultS2CPayload;
import combat.wulidam.network.s2c.WeaponDataSyncS2CPayload;
import combat.wulidam.network.c2s.ToggleWeaponC2SPayload;
import combat.wulidam.network.c2s.RequestReassembleC2SPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import combat.wulidam.network.c2s.TeleportDodgeC2SPayload;
import combat.wulidam.network.s2c.TeleportDodgeS2CPayload;
import combat.wulidam.AnimationHooks;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side mod initializer. Registers S2C packet receivers,
 * keybinds (Phase 3), HUD overlays (Phase 3), and animation handlers (Phase 4).
 */
// this is the client side starter, like keybinds hud and client packets
public class SoulsLikeCombatClient implements ClientModInitializer {
    private static final double TELEPORT_DODGE_DISTANCE = 2.0; // blocks

    private static CombatState currentCombatState = CombatState.IDLE;
    private static float currentStamina = 100.0f;
    private static float maxStamina = 100.0f;
    private static float currentPosture = 100.0f;
    private static float maxPosture = 100.0f;
    private static int ventCooldown = 0;

    // used by client mixins to decide if attack input should be ignored locally
    public static CombatState getCurrentCombatState() {
        return currentCombatState;
    }

    public static float getCurrentStamina() {
        return currentStamina;
    }

    public static float getMaxStamina() {
        return maxStamina;
    }

    public static float getCurrentPosture() {
        return currentPosture;
    }

    public static float getMaxPosture() {
        return maxPosture;
    }

    public static int getVentCooldown() {
        return ventCooldown;
    }

    @Override
    public void onInitializeClient() {
        registerS2CReceivers();

        // register keybind for toggling the sword and shield into sword + shield
        KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.soulslikecombat.toggle_weapon",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyBinding.Category.MISC
        ));

        // register keybind for dodging
        KeyBinding dodgeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.soulslikecombat.dodge",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                KeyBinding.Category.MISC
        ));

        // register keybind for parrying
        KeyBinding parryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.soulslikecombat.parry",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                KeyBinding.Category.MISC
        ));

        // register Vent Key (G)
        net.minecraft.client.option.KeyBinding ventKey = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(new net.minecraft.client.option.KeyBinding(
                "key.soulslikecombat.vent",
                org.lwjgl.glfw.GLFW.GLFW_KEY_G,
                net.minecraft.client.option.KeyBinding.Category.MISC
        ));

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ventKey.wasPressed()) {
                if (client.player != null) {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new combat.wulidam.network.c2s.VentC2SPayload());
                }
            }
        });

        // register HUD renderer
        HudRenderCallback.EVENT.register(new StaminaHudRenderer());
        HudRenderCallback.EVENT.register(new combat.wulidam.client.PostureHudRenderer());

        // Client-side state to detect hotbar selection changes
        final int[] prevSelectedSlot = new int[] { -1 };
        final int[] splitCooldown = new int[] { 0 };

        // On client tick: handle toggle key and detect hotbar selection changes to request reassembly
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Handle toggle key
            while (toggleKey.wasPressed()) {
                ClientPlayNetworking.send(new ToggleWeaponC2SPayload());
                // Temporary client-side cooldown to ignore immediate server-driven stack changes
                splitCooldown[0] = 20;
                // Initialize prevSelectedSlot to the current selected slot to avoid immediate reassemble
                try {
                    prevSelectedSlot[0] = getSelectedSlot(client);
                } catch (Throwable t) {
                    prevSelectedSlot[0] = -1;
                }
            }

            // Handle dodge-teleport key: send a 1-block left/right/back offset depending on A/D or none
            while (dodgeKey.wasPressed()) {
                Vec3d look = client.player.getRotationVec(1.0f);
                Vec3d offset;
                // Swap left/right so A => left, D => right in player-local terms
                if (client.options.leftKey.isPressed()) {
                    // left (was previously right): use (z,0,-x)
                    offset = new Vec3d(look.z, 0, -look.x).normalize().multiply(TELEPORT_DODGE_DISTANCE);
                } else if (client.options.rightKey.isPressed()) {
                    // right (was previously left): use (-z,0,x)
                    offset = new Vec3d(-look.z, 0, look.x).normalize().multiply(TELEPORT_DODGE_DISTANCE);
                } else {
                    // backward = (-x, 0, -z)
                    offset = new Vec3d(-look.x, 0, -look.z).normalize().multiply(TELEPORT_DODGE_DISTANCE);
                }
                ClientPlayNetworking.send(new TeleportDodgeC2SPayload(offset));
            }

            // handle parry key
            while (parryKey.wasPressed()) {
                ClientPlayNetworking.send(new combat.wulidam.network.c2s.ParryC2SPayload());
            }

            // Decrement cooldown
            if (splitCooldown[0] > 0) splitCooldown[0]--;

            // Detect hotbar selection change
            int currentSelected = -1;
            try {
                currentSelected = getSelectedSlot(client);
            } catch (Throwable t) {
                // fallback: leave -1
            }

            if (currentSelected != prevSelectedSlot[0]) {
                // If not still within the immediate post-split cooldown, request reassemble
                if (splitCooldown[0] == 0) {
                    // Search hotbar slots (0-8) for sword and shield and send indices to server
                    int swordSlot = -1;
                    int shieldSlot = -1;
                    for (int i = 0; i < 9; i++) {
                        ItemStack s = client.player.getInventory().getStack(i);
                        if (s.getItem() == combat.wulidam.item.ModItems.SWORD && swordSlot == -1) swordSlot = i;
                        if (s.getItem() == combat.wulidam.item.ModItems.SHIELD && shieldSlot == -1) shieldSlot = i;
                    }
                    ClientPlayNetworking.send(new RequestReassembleC2SPayload(swordSlot, shieldSlot));
                }
                prevSelectedSlot[0] = currentSelected;
            }
        });

        SoulsLikeCombat.LOGGER.info("SoulsLikeCombat client initialized");
    }

    private static int getSelectedSlot(MinecraftClient client) {
        try {
            Object inv = client.player.getInventory();
            if (inv == null) return -1;
            Class<?> cls = inv.getClass();
            // Try field 'selectedSlot'
            try {
                java.lang.reflect.Field f = cls.getDeclaredField("selectedSlot");
                f.setAccessible(true);
                return f.getInt(inv);
            } catch (NoSuchFieldException ignored) {}

            // Try method 'getSelectedSlot' or 'getSelected'
            try {
                java.lang.reflect.Method m = cls.getMethod("getSelectedSlot");
                Object v = m.invoke(inv);
                if (v instanceof Integer) return (Integer) v;
            } catch (NoSuchMethodException ignored) {}
            try {
                java.lang.reflect.Method m = cls.getMethod("getSelected");
                Object v = m.invoke(inv);
                if (v instanceof Integer) return (Integer) v;
            } catch (NoSuchMethodException ignored) {}

            // Try field 'selected' (some mappings)
            try {
                java.lang.reflect.Field f = cls.getDeclaredField("selected");
                f.setAccessible(true);
                return f.getInt(inv);
            } catch (NoSuchFieldException ignored) {}

            return -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private void registerS2CReceivers() {
        // Combat state sync — update client-side state for HUD/animations
        ClientPlayNetworking.registerGlobalReceiver(CombatStateS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                currentCombatState = payload.getState();
                currentStamina = payload.stamina();
                maxStamina = payload.maxStamina();
                currentPosture = payload.posture();
                maxPosture = payload.maxPosture();
                ventCooldown = payload.ventCooldown();

                // Store client-side combat state for HUD rendering (Phase 3)
                // For now, just log it for debugging
                SoulsLikeCombat.LOGGER.debug("Client received combat state: {} (ticks={}, combo={}, stamina={}/{})",
                        payload.getState(), payload.stateTicksRemaining(), payload.comboIndex(), currentStamina, maxStamina);
            });
        });

                // Hit result — trigger client effects (screen shake, flash, sounds)
        ClientPlayNetworking.registerGlobalReceiver(HitResultS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = context.client();
                if (client.player == null) return;

                boolean isTarget = payload.targetUuid().equals(client.player.getUuid());
                boolean isAttacker = payload.attackerUuid().equals(client.player.getUuid());

                if (isTarget) {
                    if (payload.hitType() == HitResultS2CPayload.HIT_PARRIED) {
                        // We were parried by someone else!
                        // Maybe a specific effect here?
                    } else if (payload.hitType() == HitResultS2CPayload.HIT_NORMAL || payload.hitType() == HitResultS2CPayload.HIT_INTERRUPTED) {
                        // We were hit
                        // Removed showFloatingItem as it triggers an unwanted sound
                    }
                }

                if (isAttacker) {
                    if (payload.hitType() == HitResultS2CPayload.HIT_PARRIED) {
                        // Our attack was parried!
                    }
                }

                SoulsLikeCombat.LOGGER.debug("Client received hit result: type={}, damage={}",
                        payload.hitType(), payload.damageDealt());
            });
        });

        // Weapon data sync — populate client-side weapon registry
        ClientPlayNetworking.registerGlobalReceiver(WeaponDataSyncS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                for (WeaponDataSyncS2CPayload.WeaponDataEntry entry : payload.entries()) {
                    WeaponData data = WeaponDataSyncS2CPayload.toWeaponData(entry);
                    WeaponRegistry.registerClientWeaponData(data);
                }
                SoulsLikeCombat.LOGGER.info("Client received {} weapon data entries",
                        payload.entries().size());
            });
        });

        // Teleport-dodge S2C: play client-side dodge animation/effects (or bump if blocked)
        ClientPlayNetworking.registerGlobalReceiver(TeleportDodgeS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.bumped()) {
                    AnimationHooks.playDodgeBump();
                } else {
                    AnimationHooks.playBlenderDodge();
                }
            });
        });
    }
}
