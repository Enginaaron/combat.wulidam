package combat.wulidam.item;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.util.Identifier;

// heavy sword item, uses heavy sword stats
public class HeavyswordItem extends SoulsWeaponItem {
    public static final Identifier WEAPON_DATA_ID = Identifier.of(SoulsLikeCombat.MOD_ID, "heavysword");

    public HeavyswordItem(Settings settings) {
        super(settings, WEAPON_DATA_ID);
    }
}
