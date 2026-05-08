package combat.wulidam.combat;

import net.minecraft.util.Identifier;
import java.util.List;

/**
 * Holds all combat-related stats for a weapon type.
 * Loaded from JSON files in data/soulslikecombat/weapon_data/.
 * This is the single source of truth for weapon behavior — change a JSON file
 * and the weapon's feel changes without recompiling.
 */
public record WeaponData(
        /** Unique identifier for this weapon data (e.g. soulslikecombat:longsword). */
        Identifier id,

        // --- Damage ---
        /** Base damage dealt per hit (before combo multipliers). */
        float baseDamage,
        /** Maximum range of the attack hitbox (in blocks). */
        float attackRange,

        // --- Attack Timing ---
        /** Ticks of wind-up before the damage frame begins. */
        int windUpTicks,
        /** Ticks where the hitbox is active and can deal damage. */
        int activeTicks,
        /** Ticks of recovery after the attack — player is vulnerable. */
        int recoveryTicks,

        // --- Parry ---
        /** Ticks the parry window stays active. */
        int parryWindowTicks,
        /** Stun ticks applied to the attacker on successful parry. */
        int parryStunTicks,
        /** Knockback strength applied to attacker on successful parry. */
        float parryKnockback,
        /** Stun ticks applied to self on failed parry (window expired). */
        int parryFailStunTicks,
        /** Cooldown ticks before parry can be used again. */
        int parryCooldownTicks,

        // --- Dodge ---
        /** Total duration of the dodge in ticks. */
        int dodgeTicks,
        /** Number of invincibility frames during the dodge. */
        int dodgeIFrames,
        /** Distance the player moves during the dodge (in blocks). */
        float dodgeDistance,
        /** Cooldown ticks before dodge can be used again. */
        int dodgeCooldownTicks,

        // --- Hit Stun ---
        /** Stun ticks applied to a target when they are hit normally. */
        int hitStunTicks,
        /** Extra stun ticks applied if the target was mid-attack (punish). */
        int interruptStunTicks,

        // --- Combo ---
        /** Number of attacks in the full combo chain. */
        int comboCount,
        /** Ticks after recovery ends to input the next combo attack. */
        int comboWindowTicks,
        /** Per-step overrides for each combo attack. */
        List<ComboAttack> comboAttacks
) {
    /**
     * Get the combo attack data for a specific step, falling back to defaults.
     */
    public ComboAttack getComboAttack(int index) {
        if (comboAttacks != null && index >= 0 && index < comboAttacks.size()) {
            return comboAttacks.get(index);
        }
        return ComboAttack.defaultAttack();
    }

    /**
     * Get effective wind-up ticks for a specific combo step.
     */
    public int getWindUpForCombo(int comboIndex) {
        return getComboAttack(comboIndex).getEffectiveWindUp(windUpTicks);
    }

    /**
     * Get effective active ticks for a specific combo step.
     */
    public int getActiveForCombo(int comboIndex) {
        return getComboAttack(comboIndex).getEffectiveActive(activeTicks);
    }

    /**
     * Get effective damage for a specific combo step.
     */
    public float getDamageForCombo(int comboIndex) {
        return baseDamage * getComboAttack(comboIndex).damageMultiplier();
    }

    /**
     * Get effective range for a specific combo step.
     */
    public float getRangeForCombo(int comboIndex) {
        return attackRange * getComboAttack(comboIndex).rangeMultiplier();
    }
}
