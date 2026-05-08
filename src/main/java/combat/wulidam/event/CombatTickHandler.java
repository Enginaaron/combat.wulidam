package combat.wulidam.event;

import combat.wulidam.combat.AttackHandler;
import combat.wulidam.combat.CombatState;
import combat.wulidam.combat.CombatStateManager;
import combat.wulidam.combat.PlayerCombatData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

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
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                // Tick the combat state machine
                CombatStateManager.tick(player);

                // Process active attack frames
                PlayerCombatData data = CombatStateManager.getOrCreate(player);
                if (data.getCurrentState() == CombatState.ATTACKING) {
                    AttackHandler.processAttackTick(player);
                }
            }
        });
    }
}
