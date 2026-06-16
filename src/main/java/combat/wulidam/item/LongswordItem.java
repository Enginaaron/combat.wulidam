package combat.wulidam.item;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.util.Identifier;

// longsword item, connects this item to longsword stats
public class LongswordItem extends SoulsWeaponItem {
    public static final Identifier WEAPON_DATA_ID = Identifier.of(SoulsLikeCombat.MOD_ID, "longsword");

    public LongswordItem(Settings settings) {
        super(settings, WEAPON_DATA_ID);
    }
}
