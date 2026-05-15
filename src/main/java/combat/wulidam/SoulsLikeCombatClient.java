package combat.wulidam;

import combat.wulidam.combat.WeaponData;
import combat.wulidam.combat.WeaponRegistry;
import combat.wulidam.network.s2c.CombatStateS2CPayload;
import combat.wulidam.network.s2c.HitResultS2CPayload;
import combat.wulidam.network.s2c.WeaponDataSyncS2CPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side mod initializer. Registers S2C packet receivers,
 * keybinds (Phase 3), HUD overlays (Phase 3), and animation handlers (Phase 4).
 */
public class SoulsLikeCombatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        registerS2CReceivers();

        SoulsLikeCombat.LOGGER.info("SoulsLikeCombat client initialized");
    }

    private void registerS2CReceivers() {
        // Combat state sync — update client-side state for HUD/animations
        ClientPlayNetworking.registerGlobalReceiver(CombatStateS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
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
    }
}
