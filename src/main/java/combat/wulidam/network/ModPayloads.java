package combat.wulidam.network;

import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.combat.CombatStateManager;
import combat.wulidam.combat.DodgeHandler;
import combat.wulidam.network.c2s.AttackC2SPayload;
import combat.wulidam.network.c2s.DodgeC2SPayload;
import combat.wulidam.network.c2s.ParryC2SPayload;
import combat.wulidam.network.c2s.ToggleWeaponC2SPayload;
import combat.wulidam.network.s2c.CombatStateS2CPayload;
import combat.wulidam.network.s2c.HitResultS2CPayload;
import combat.wulidam.network.s2c.WeaponDataSyncS2CPayload;
import combat.wulidam.item.ModItems;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import combat.wulidam.SplitTracker;
import java.util.UUID;

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
        PayloadTypeRegistry.playC2S().register(ToggleWeaponC2SPayload.ID, ToggleWeaponC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(combat.wulidam.network.c2s.RequestReassembleC2SPayload.ID, combat.wulidam.network.c2s.RequestReassembleC2SPayload.CODEC);

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

        // Toggle weapon: convert SwordAndShield into sword + shield
        ServerPlayNetworking.registerGlobalReceiver(ToggleWeaponC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                ItemStack main = player.getMainHandStack();
                // Only operate when holding the combined SwordAndShield in main hand
                if (main.getItem() == ModItems.SWORD_AND_SHIELD) {
                    // Create new stacks (do not copy NBT to avoid mapping-specific APIs)
                    ItemStack newMain = new ItemStack(ModItems.SWORD, 1);
                    ItemStack newOff = new ItemStack(ModItems.SHIELD, 1);

                    // Replace main hand
                    player.setStackInHand(Hand.MAIN_HAND, newMain);

                    // If offhand empty, put the shield there; otherwise try inventory, else drop
                    if (player.getOffHandStack().isEmpty()) {
                        player.setStackInHand(Hand.OFF_HAND, newOff);
                    } else if (player.getInventory().insertStack(newOff)) {
                        // inserted into inventory
                    } else {
                        player.dropItem(newOff, false);
                    }

                    // Track that this player has a split pair for reassembly and store their current hotbar slot
                    // Save the actual ItemStack reference present in the player's main hand after splitting
                    SplitTracker.markSplit(player.getUuid(), player.getMainHandStack());

                    SoulsLikeCombat.LOGGER.debug("Toggled SwordAndShield for {}", player.getName().getString());
                }
            });
        });

        SoulsLikeCombat.LOGGER.info("Registered SoulsLikeCombat server receivers");
        // Reassembly request from client when player switched hotbar slot
        ServerPlayNetworking.registerGlobalReceiver(combat.wulidam.network.c2s.RequestReassembleC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                int swordSlot = payload.swordSlot();
                int shieldSlot = payload.shieldSlot();

                // Prefer reassembling into the sword hotbar slot if present
                if (swordSlot >= 0) {
                    ItemStack s = player.getInventory().getStack(swordSlot);
                    ItemStack off = player.getOffHandStack();
                    if (s.getItem() == ModItems.SWORD && off.getItem() == ModItems.SHIELD) {
                        // Remove one shield from offhand and replace sword slot with combined
                        player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                        player.getInventory().setStack(swordSlot, new ItemStack(ModItems.SWORD_AND_SHIELD, 1));
                        SplitTracker.clearSplit(player.getUuid());
                        SoulsLikeCombat.LOGGER.debug("Reassembled SwordAndShield for {} into hotbar slot {}", player.getName().getString(), swordSlot);
                        return;
                    }
                }

                // Fallback: if both are in main+offhand, combine there
                ItemStack main = player.getMainHandStack();
                ItemStack off = player.getOffHandStack();
                if (SplitTracker.isMarked(player.getUuid()) && main.getItem() == ModItems.SWORD && off.getItem() == ModItems.SHIELD) {
                    ItemStack combined = new ItemStack(ModItems.SWORD_AND_SHIELD, 1);

                    player.setStackInHand(Hand.MAIN_HAND, combined);
                    player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                    SplitTracker.clearSplit(player.getUuid());
                    SoulsLikeCombat.LOGGER.debug("Reassembled SwordAndShield for {} (client request)", player.getName().getString());
                }
            });
        });

        SoulsLikeCombat.LOGGER.info("Registered all SoulsLikeCombat server receivers");
    }
}
