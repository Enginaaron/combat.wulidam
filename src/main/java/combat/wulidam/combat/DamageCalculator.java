package combat.wulidam.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Computes and applies final damage, knockback, and hit stun.
 */
public class DamageCalculator {

    public static void applyDamage(ServerPlayerEntity attacker, LivingEntity target,
                                   float baseDamage, WeaponData weaponData) {
        DamageSource source = attacker.getDamageSources().playerAttack(attacker);
        ServerWorld world = attacker.getEntityWorld();

        target.damage(world, source, baseDamage);
        applyWeaponKnockback(attacker, target, weaponData);
    }

    private static void applyWeaponKnockback(ServerPlayerEntity attacker,
                                             LivingEntity target,
                                             WeaponData weaponData) {
        double dx = target.getX() - attacker.getX();
        double dz = target.getZ() - attacker.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance > 0.01) {
            float knockbackStrength = getKnockbackStrength(weaponData);
            double knockbackX = (dx / distance) * knockbackStrength;
            double knockbackZ = (dz / distance) * knockbackStrength;

            target.setVelocity(target.getVelocity().add(knockbackX, 0.1, knockbackZ));
            target.velocityDirty = true;
        }
    }

    private static float getKnockbackStrength(WeaponData weaponData) {
        float speedFactor = Math.min(weaponData.windUpTicks() / 5.0f, 2.0f);
        return 0.3f + (0.2f * speedFactor);
    }
}
