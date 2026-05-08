package combat.wulidam.item;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.util.Identifier;

public class ScytheItem extends SoulsWeaponItem {
    public static final Identifier WEAPON_DATA_ID =
            Identifier.of(SoulsLikeCombat.MOD_ID, "scythe");

    public ScytheItem(Settings settings) {
        super(settings, WEAPON_DATA_ID);
    }
}
