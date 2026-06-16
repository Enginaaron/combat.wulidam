package combat.wulidam.item;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.util.Identifier;

/**
 * Sword and Shield: a single combined item (offhand disabled for the mod).
 */
// combined sword and shield item before it gets split
public class SwordAndShieldItem extends SoulsWeaponItem {
    public static final Identifier WEAPON_DATA_ID =
            Identifier.of(SoulsLikeCombat.MOD_ID, "sword_and_shield");

    public SwordAndShieldItem(Settings settings) {
        super(settings, WEAPON_DATA_ID);
    }
}
