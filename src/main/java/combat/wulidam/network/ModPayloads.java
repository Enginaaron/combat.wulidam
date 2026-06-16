package combat.wulidam.network;

import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.combat.CombatStateManager;
import combat.wulidam.combat.DodgeHandler;
import combat.wulidam.network.c2s.*;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;

import java.util.UUID;

/**
 * Registers all custom payload types (C2S and S2C) and their server-side receivers.
 * Payload type registration must happen before receiver registration.
 */
// wires up all the packets so client and server can talk
public class ModPayloads {

    // packet ids/codecs only - no logic here, just wiring to fabric registry
    /**
     * Register all payload types with the PayloadTypeRegistry.
     * Must be called during mod initialization, before receivers are registered.
     */
    public static void registerPayloadTypes() {
        // --- Client-to-Server ---
        PayloadTypeRegistry.playC2S().register(AttackC2SPayload.ID, AttackC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ParryC2SPayload.ID, ParryC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DodgeC2SPayload.ID, DodgeC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(VentC2SPayload.ID, VentC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleWeaponC2SPayload.ID, ToggleWeaponC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(combat.wulidam.network.c2s.RequestReassembleC2SPayload.ID, combat.wulidam.network.c2s.RequestReassembleC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportDodgeC2SPayload.ID, TeleportDodgeC2SPayload.CODEC);

        // --- Server-to-Client ---
        PayloadTypeRegistry.playS2C().register(CombatStateS2CPayload.ID, CombatStateS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HitResultS2CPayload.ID, HitResultS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WeaponDataSyncS2CPayload.ID, WeaponDataSyncS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(combat.wulidam.network.s2c.TeleportDodgeS2CPayload.ID, combat.wulidam.network.s2c.TeleportDodgeS2CPayload.CODEC);

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

        // Teleport-dodge intent: client requests a teleport offset (validated server-side)
        ServerPlayNetworking.registerGlobalReceiver(TeleportDodgeC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                boolean accepted = CombatStateManager.requestDodge(player);
                if (accepted) {
                    Vec3d dir = payload.getDirection();
                    ServerWorld world = player.getEntityWorld();

                    // If the client sent a zero-length vector, fall back to a small backward hop
                    double maxDist = dir.length();
                    Vec3d startPos = new Vec3d(player.getX(), player.getY(), player.getZ());
                    Vec3d norm = maxDist > 1e-6 ? dir.normalize() : new Vec3d(0, 0, 0);

                    // Step along the direction and find the furthest non-colliding position up to maxDist
                    double step = 0.25; // quarter-block steps for decent precision
                    Vec3d lastSafe = null;
                    for (double d = step; d <= maxDist + 1e-6; d += step) {
                        Vec3d candidate = startPos.add(norm.multiply(d));
                        Box candidateBox = player.getBoundingBox().offset(candidate.x - player.getX(), candidate.y - player.getY(), candidate.z - player.getZ());
                        boolean collides = world.getBlockCollisions(null, candidateBox).iterator().hasNext();
                        if (!collides) {
                            lastSafe = candidate;
                        } else {
                            // Hit a collision — stop searching further
                            break;
                        }
                    }

                    if (lastSafe != null) {
                        player.requestTeleport(lastSafe.x, lastSafe.y, lastSafe.z);
                        player.velocityDirty = true;
                        // Send S2C payload with bumped=false to indicate successful dodge
                        ServerPlayNetworking.send(player, new combat.wulidam.network.s2c.TeleportDodgeS2CPayload(false));
                        SoulsLikeCombat.LOGGER.debug("Teleport-dodge executed for {} to {}", player.getName().getString(), lastSafe);
                    } else {
                        // No safe spot found: keep player in place and notify client to play bump effect
                        ServerPlayNetworking.send(player, new combat.wulidam.network.s2c.TeleportDodgeS2CPayload(true));
                        SoulsLikeCombat.LOGGER.debug("Teleport-dodge blocked for {}", player.getName().getString());
                    }
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

        // on vent use
        ServerPlayNetworking.registerGlobalReceiver(VentC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                combat.wulidam.combat.VentHandler.executeVent(player);
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
