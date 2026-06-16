package combat.wulidam.item;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.util.Identifier;

/**
 * Sword and Shield: a single combined item (offhand disabled for the mod).
 */
// split sword item, reuses the combo stats from sword and shield
public class SwordItem extends SoulsWeaponItem {
    // Reuse combined weapon data for the split Sword so dodge/attack parameters
    // remain valid even when the player is holding the separated Sword item.
    public static final Identifier WEAPON_DATA_ID = SwordAndShieldItem.WEAPON_DATA_ID;

    public SwordItem(Settings settings) {
        super(settings, WEAPON_DATA_ID);
    }
}
