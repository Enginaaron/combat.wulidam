package combat.wulidam.network;

import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.combat.CombatStateManager;
import combat.wulidam.combat.DodgeHandler;
import combat.wulidam.network.c2s.AttackC2SPayload;
import combat.wulidam.network.c2s.DodgeC2SPayload;
import combat.wulidam.network.c2s.ParryC2SPayload;
import combat.wulidam.network.s2c.CombatStateS2CPayload;
import combat.wulidam.network.s2c.HitResultS2CPayload;
import combat.wulidam.network.s2c.WeaponDataSyncS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Registers all custom payload types (C2S and S2C) and their server-side receivers.
 * Payload type registration must happen before receiver registration.
 */
public class ModPayloads {

    /**
     * Register all payload types with the PayloadTypeRegistry.
     * Must be called during mod initialization, before receivers are registered.
     */
    public static void registerPayloadTypes() {
        // --- Client-to-Server ---
        PayloadTypeRegistry.playC2S().register(AttackC2SPayload.ID, AttackC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ParryC2SPayload.ID, ParryC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DodgeC2SPayload.ID, DodgeC2SPayload.CODEC);

        // --- Server-to-Client ---
        PayloadTypeRegistry.playS2C().register(CombatStateS2CPayload.ID, CombatStateS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HitResultS2CPayload.ID, HitResultS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WeaponDataSyncS2CPayload.ID, WeaponDataSyncS2CPayload.CODEC);

        SoulsLikeCombat.LOGGER.info("Registered all SoulsLikeCombat payload types");
    }

    /**
     * Register server-side receivers for C2S packets.
     * Must be called after payload types are registered.
     */
    public static void registerServerReceivers() {
        // Attack intent: player wants to attack
        ServerPlayNetworking.registerGlobalReceiver(AttackC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                boolean accepted = CombatStateManager.requestAttack(player);
                if (accepted) {
                    SoulsLikeCombat.LOGGER.debug("Attack request accepted for {}", player.getName().getString());
                }
            });
        });

        // Parry intent: player wants to parry
        ServerPlayNetworking.registerGlobalReceiver(ParryC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                boolean accepted = CombatStateManager.requestParry(player);
                if (accepted) {
                    SoulsLikeCombat.LOGGER.debug("Parry request accepted for {}", player.getName().getString());
                }
            });
        });

        // Dodge intent: player wants to dodge in a direction
        ServerPlayNetworking.registerGlobalReceiver(DodgeC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                boolean accepted = CombatStateManager.requestDodge(player);
                if (accepted) {
                    DodgeHandler.executeDodge(player, payload.getDirection());
                    SoulsLikeCombat.LOGGER.debug("Dodge request accepted for {}", player.getName().getString());
                }
            });
        });

        SoulsLikeCombat.LOGGER.info("Registered SoulsLikeCombat server receivers");
    }
}
