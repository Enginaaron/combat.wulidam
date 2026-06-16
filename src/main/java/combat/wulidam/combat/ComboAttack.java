package combat.wulidam.combat;

/**
 * Per-attack overrides within a combo chain.
 * Each step in a weapon's combo can have different damage, range, and timing.
 */
// one hit in a combo, with its own damage range and timing stuff
public record ComboAttack(
        /** Multiplier applied to the weapon's base damage for this combo step. */
        float damageMultiplier,

        /** Multiplier applied to the weapon's base range for this combo step. */
        float rangeMultiplier,

        /** Override wind-up ticks for this combo step (-1 = use weapon default). */
        int windUpTicks,

        /** Override active ticks for this combo step (-1 = use weapon default). */
        int activeTicks
) {
    /**
     * @return a default combo attack with no overrides (1x multipliers, use weapon defaults).
     */
    public static ComboAttack defaultAttack() {
        return new ComboAttack(1.0f, 1.0f, -1, -1);
    }

    /**
     * Get effective wind-up ticks, falling back to weapon default if not overridden.
     */
    public int getEffectiveWindUp(int weaponDefault) {
        return windUpTicks >= 0 ? windUpTicks : weaponDefault;
    }

    /**
     * Get effective active ticks, falling back to weapon default if not overridden.
     */
    public int getEffectiveActive(int weaponDefault) {
        return activeTicks >= 0 ? activeTicks : weaponDefault;
    }
}
