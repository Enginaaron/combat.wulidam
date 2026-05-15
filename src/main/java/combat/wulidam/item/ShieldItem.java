package combat.wulidam.item;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.util.Identifier;

/**
 * Sword and Shield: a single combined item (offhand disabled for the mod).
 */
public class ShieldItem extends SoulsWeaponItem {
    public static final Identifier WEAPON_DATA_ID =
            Identifier.of(SoulsLikeCombat.MOD_ID, "shield");

    public ShieldItem(Settings settings) {
        super(settings, WEAPON_DATA_ID);
    }
}
