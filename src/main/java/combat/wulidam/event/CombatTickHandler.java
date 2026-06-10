package combat.wulidam.event;

import combat.wulidam.combat.AttackHandler;
import combat.wulidam.combat.CombatState;
import combat.wulidam.combat.CombatStateManager;
import combat.wulidam.combat.PlayerCombatData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.SplitTracker;
import combat.wulidam.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

/**
 * Registers and handles the server tick event for the combat system.
 * Each tick, this updates every player's combat state machine and
 * processes active attack frames.
 */
public class CombatTickHandler {

    /**
     * Register the server tick event. Called once from ModInitializer.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
                    if (entity instanceof MobEntity mob) {
                        CombatStateManager.tickMobStun(mob);
                    }
                }
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // Tick the combat state machine
                CombatStateManager.tick(player);

                // Process active attack frames
                PlayerCombatData data = CombatStateManager.getOrCreate(player);
                if (data.getCurrentState() == CombatState.ATTACKING) {
                    AttackHandler.processAttackTick(player);
                }

                // Reassemble SwordAndShield if player is holding the split sword+shield created by toggle
                ItemStack main = player.getMainHandStack();
                ItemStack off = player.getOffHandStack();
                // Only reassemble if this player was the one who split previously
                if (SplitTracker.isMarked(player.getUuid()) && main.getItem() == ModItems.SWORD && off.getItem() == ModItems.SHIELD) {
                    ItemStack saved = SplitTracker.getSavedStack(player.getUuid());
                    // Reference equality: if the player's current main hand stack object is different,
                    // they switched hotbar slot (or the stack changed) and we should reassemble.
                    if (saved != null && player.getMainHandStack() != saved) {
                        ItemStack combined = new ItemStack(ModItems.SWORD_AND_SHIELD, 1);

                        player.setStackInHand(Hand.MAIN_HAND, combined);
                        player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
                        SplitTracker.clearSplit(player.getUuid());
                        SoulsLikeCombat.LOGGER.debug("Reassembled SwordAndShield for {} (slot switch)", player.getName().getString());
                    }
                }
            }
        });
    }
}
