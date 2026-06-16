package combat.wulidam.item;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.util.Identifier;

// dagger item, it just points to dagger weapon data
public class DaggerItem extends SoulsWeaponItem {
    public static final Identifier WEAPON_DATA_ID =
            Identifier.of(SoulsLikeCombat.MOD_ID, "dagger");

    public DaggerItem(Settings settings) {
        super(settings, WEAPON_DATA_ID);
    }
}
