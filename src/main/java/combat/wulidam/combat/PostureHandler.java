package combat.wulidam.combat;

import net.minecraft.server.network.ServerPlayerEntity;

public class PostureHandler {
    public static final float PARRY_REGAIN = 0.20f;
    public static final float DODGE_REGAIN = 0.10f;
    public static final float PARRIED_LOSS = 0.25f;
    public static final float DAMAGE_TO_POSTURE_MULT = 1.4f; // 30% nerf from 2.0f
    
    public static final int STUN_DURATION = 30; // 1.5s
    public static final int IMMUNITY_DURATION = 100; // 5s

    public static void onParry(ServerPlayerEntity player, PlayerCombatData data) {
        data.setPosture(data.getPosture() + (data.getMaxPosture() * PARRY_REGAIN));
    }

    public static void onDodge(ServerPlayerEntity player, PlayerCombatData data) {
        data.setPosture(data.getPosture() + (data.getMaxPosture() * DODGE_REGAIN));
    }

    public static void onParried(ServerPlayerEntity player, PlayerCombatData data) {
        data.setPosture(data.getPosture() - (data.getMaxPosture() * PARRIED_LOSS));
        checkPostureBreak(player, data);
    }

    public static void onHit(ServerPlayerEntity player, PlayerCombatData data, float damage) {
        if (data.getPostureImmunityTicks() > 0) return;
        
        float postureLoss = damage * DAMAGE_TO_POSTURE_MULT;
        data.setPosture(data.getPosture() - postureLoss);
        checkPostureBreak(player, data);
    }

    public static void checkPostureBreak(ServerPlayerEntity player, PlayerCombatData data) {
        if (data.getPosture() <= 0 && data.getPostureImmunityTicks() == 0) {
            data.setState(CombatState.STUNNED, STUN_DURATION);
            data.setPostureImmunityTicks(IMMUNITY_DURATION);
            CombatStateManager.syncStateToClient(player, data);
        }
    }
}
