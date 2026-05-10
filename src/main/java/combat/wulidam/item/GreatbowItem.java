package combat.wulidam.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class GreatbowItem extends BowItem {
    private static final int DRAW_DURATION_TICKS = 45; // this is the draw speed. more explained below
    private static final float VELOCITY_MULTIPLIER = 1.75f; // since arrow damage is calculated using the arrow velocity, the greatbow trades its cooldown for more damage.

    public GreatbowItem(Settings settings) {
        super(settings);
    }

    @Override
    protected void shoot(LivingEntity shooter, ProjectileEntity projectile, int index, float speed,
                         float divergence, float yaw, @Nullable LivingEntity target) {
        super.shoot(shooter, projectile, index, speed * VELOCITY_MULTIPLIER, divergence, yaw, target);
    }

    public static float getGreatbowPullProgress(int useTicks) {
        float progress = (float) useTicks / DRAW_DURATION_TICKS;
        progress = (progress * progress + progress * 2.0f) / 3.0f;
        return MathHelper.clamp(progress, 0.0f, 1.0f);
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, net.minecraft.world.World world, LivingEntity user, int remainingUseTicks) {
        int useTicks = this.getMaxUseTime(stack, user) - remainingUseTicks;
        int adjustedUseTicks = MathHelper.floor(getGreatbowPullProgress(useTicks) * 20.0f);
        int adjustedRemainingUseTicks = this.getMaxUseTime(stack, user) - adjustedUseTicks;
        return super.onStoppedUsing(stack, world, user, adjustedRemainingUseTicks);
    }
}
