package combat.wulidam;

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
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
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
public class SoulsLikeCombatClient implements ClientModInitializer {
    private static final double TELEPORT_DODGE_DISTANCE = 2.0; // blocks

    private static CombatState currentCombatState = CombatState.IDLE;

    // used by client mixins to decide if attack input should be ignored locally
    public static CombatState getCurrentCombatState() {
        return currentCombatState;
    }

    @Override
    public void onInitializeClient() {
        registerS2CReceivers();

        // Register keybind for toggling the SwordAndShield into sword + shield
        KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.soulslikecombat.toggle_weapon",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyBinding.Category.MISC
        ));

        // Register keybind for dodging
        KeyBinding dodgeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.soulslikecombat.dodge",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                KeyBinding.Category.MISC
        ));

        // Client-side state to detect hotbar/main-hand changes
        final ItemStack[] prevMain = new ItemStack[] { ItemStack.EMPTY };
        final int[] splitCooldown = new int[] { 0 };

        // On client tick: handle toggle key and detect main-hand changes to request reassembly
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Handle toggle key
            while (toggleKey.wasPressed()) {
                ClientPlayNetworking.send(new ToggleWeaponC2SPayload());
                // Temporary client-side cooldown to ignore immediate server-driven stack changes
                splitCooldown[0] = 20;
                prevMain[0] = client.player.getMainHandStack();
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

            // Decrement cooldown
            if (splitCooldown[0] > 0) splitCooldown[0]--;

            // Detect main-hand stack reference change (hotbar switch or selection change)
            ItemStack currentMain = client.player.getMainHandStack();
            if (prevMain[0] != currentMain) {
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
                prevMain[0] = currentMain;
            }
        });

        SoulsLikeCombat.LOGGER.info("SoulsLikeCombat client initialized");
    }

    private void registerS2CReceivers() {
        // Combat state sync — update client-side state for HUD/animations
        ClientPlayNetworking.registerGlobalReceiver(CombatStateS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                currentCombatState = payload.getState();

                // Store client-side combat state for HUD rendering (Phase 3)
                // For now, just log it for debugging
                SoulsLikeCombat.LOGGER.debug("Client received combat state: {} (ticks={}, combo={})",
                        payload.getState(), payload.stateTicksRemaining(), payload.comboIndex());
            });
        });

        // Hit result — trigger client effects (screen shake, flash, sounds)
        ClientPlayNetworking.registerGlobalReceiver(HitResultS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                // Phase 3 will use this to trigger screen effects
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
