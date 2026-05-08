package combat.wulidam.combat;

/**
 * Represents all possible states a player can be in during souls-like combat.
 * The combat system is a state machine — players transition between these states
 * based on their inputs and interactions with other entities.
 */
public enum CombatState {
    /** Default state, player can initiate any action here (attack, parry, dodge). */
    IDLE,

    /** Player commits and locks into attack, animation wind-up before the damage frame.*/
    WIND_UP,

    /** Active damage frames where weapon hitbox is live, can deal damage to targets. */
    ATTACKING,

    /** Post-attack recovery, player is vulnerable and cannot act. */
    RECOVERY,

    /** Active parry window, a short window where incoming attacks are deflected. */
    PARRYING,

    /** Brief reward state after a successful parry. Could enable a riposte in the future. */
    PARRY_SUCCESS,

    /** Failed parry — window expired without deflecting. Player is briefly stunned. */
    PARRY_FAIL,

    /** Dodge active — i-frames are active, player moves in dodge direction. */
    DODGING,

    /** Hit-stunned — player was hit and cannot act for a short duration. */
    STUNNED;

    /**
     * @return true if the player can initiate a new action from this state.
     */
    public boolean canAct() {
        return this == IDLE || this == PARRY_SUCCESS;
    }

    /**
     * @return true if the player is in a state where they are vulnerable to extra punishment.
     */
    public boolean isVulnerable() {
        return this == RECOVERY || this == PARRY_FAIL || this == WIND_UP;
    }

    /**
     * @return true if the player has active invincibility frames.
     */
    public boolean hasIFrames() {
        return this == DODGING;
    }
}
