package combat.wulidam.combat;

import combat.wulidam.sound.ModSounds;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;

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

            // Posture break feedback
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POSTURE_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
            if (player.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.5, player.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
            }

            CombatStateManager.syncStateToClient(player, data);
        }
    }
}
