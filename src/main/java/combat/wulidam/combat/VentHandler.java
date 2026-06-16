package combat.wulidam.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

// makes the vent move that spends stamina and pushes enemies away
public class VentHandler {
    public static final float STAMINA_COST = 30.0f;
    public static final int COOLDOWN = 200; // 10 seconds
    public static final double RADIUS = 5.0;
    public static final double KNOCKBACK_STRENGTH = 2.0;

    public static void executeVent(ServerPlayerEntity player) {
        PlayerCombatData data = CombatStateManager.getOrCreate(player);
        
        if (data.getVentCooldownRemaining() > 0) return;
        if (data.getStamina() < STAMINA_COST) return;

        data.setStamina(data.getStamina() - STAMINA_COST);
        data.setVentCooldownRemaining(COOLDOWN);

        Box box = player.getBoundingBox().expand(RADIUS);
        List<Entity> entities = player.getEntityWorld().getOtherEntities(player, box);

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity target && player.squaredDistanceTo(target) <= RADIUS * RADIUS) {
                Vec3d knockbackDir = new Vec3d(target.getX() - player.getX(), target.getY() - player.getY(), target.getZ() - player.getZ()).normalize();
                if (knockbackDir.lengthSquared() < 0.01) {
                    knockbackDir = new Vec3d(0, 0.1, 0);
                }
                target.takeKnockback(KNOCKBACK_STRENGTH, -knockbackDir.x, -knockbackDir.z);
            }
        }

        CombatStateManager.syncStateToClient(player, data);
    }
}
