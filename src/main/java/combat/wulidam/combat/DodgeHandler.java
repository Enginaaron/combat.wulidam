package combat.wulidam.combat;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Handles dodge execution — applies movement velocity and manages i-frames.
 */
// pushes the player for a normal dodge
public class DodgeHandler {

    public static void executeDodge(ServerPlayerEntity player, Vec3d direction) {
        PlayerCombatData data = CombatStateManager.getOrCreate(player);
        WeaponData weaponData = WeaponRegistry.getWeaponData(data.getCurrentWeaponDataId());
        if (weaponData == null) return;

        Vec3d dodgeVelocity;

        if (direction.lengthSquared() < 0.01) {
            Vec3d lookDir = player.getRotationVec(1.0f);
            dodgeVelocity = new Vec3d(-lookDir.x, 0, -lookDir.z).normalize()
                    .multiply(weaponData.dodgeDistance() / weaponData.dodgeTicks());
        } else {
            dodgeVelocity = direction.normalize()
                    .multiply(weaponData.dodgeDistance() / weaponData.dodgeTicks());
        }

        player.setVelocity(dodgeVelocity.x, 0.05, dodgeVelocity.z);
        player.velocityDirty = true;
    }

    public static boolean hasIFrames(ServerPlayerEntity player) {
        PlayerCombatData data = CombatStateManager.get(player.getUuid());
        if (data == null) return false;
        return data.getCurrentState() == CombatState.DODGING;
    }
}
