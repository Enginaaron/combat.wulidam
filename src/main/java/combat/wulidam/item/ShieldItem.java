package combat.wulidam.item;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.util.Identifier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.item.Item;
import net.minecraft.world.World;

/**
 * Sword and Shield: a single combined item (offhand disabled for the mod).
 */
public class ShieldItem extends SoulsWeaponItem {
    public static final Identifier WEAPON_DATA_ID =
            Identifier.of(SoulsLikeCombat.MOD_ID, "shield");

    public ShieldItem(Settings settings) {
        super(settings, WEAPON_DATA_ID);
    }

    @Override
    public UseAction getUseAction(ItemStack stack){
        return UseAction.BLOCK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user){
        return 72000;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand){
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }
}
