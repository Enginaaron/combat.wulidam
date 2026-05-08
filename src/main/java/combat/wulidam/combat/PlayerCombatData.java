package combat.wulidam.combat;

import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Per-player combat state tracking. One instance per player is maintained
 * by the CombatStateManager on the server side.
 */
public class PlayerCombatData {
    private final UUID playerUuid;

    // --- State Machine ---
    private CombatState currentState = CombatState.IDLE;
    private int stateTicksRemaining = 0;

    // --- Combo ---
    private int comboIndex = 0;
    private long lastRecoveryEndTick = 0; // server tick when recovery last ended (for combo window)

    // --- Cooldowns ---
    private int parryCooldownRemaining = 0;
    private int dodgeCooldownRemaining = 0;

    // --- Weapon ---
    private Identifier currentWeaponDataId = null;

    // --- Flags ---
    /** True if an attack in the current active frames has already dealt damage to a target. */
    private boolean attackHitConnected = false;

    public PlayerCombatData(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    // --- Getters ---

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public CombatState getCurrentState() {
        return currentState;
    }

    public int getStateTicksRemaining() {
        return stateTicksRemaining;
    }

    public int getComboIndex() {
        return comboIndex;
    }

    public long getLastRecoveryEndTick() {
        return lastRecoveryEndTick;
    }

    public int getParryCooldownRemaining() {
        return parryCooldownRemaining;
    }

    public int getDodgeCooldownRemaining() {
        return dodgeCooldownRemaining;
    }

    public Identifier getCurrentWeaponDataId() {
        return currentWeaponDataId;
    }

    public boolean hasAttackHitConnected() {
        return attackHitConnected;
    }

    // --- Setters / Mutations ---

    public void setState(CombatState state, int ticks) {
        this.currentState = state;
        this.stateTicksRemaining = ticks;
        this.attackHitConnected = false;
    }

    public void setComboIndex(int index) {
        this.comboIndex = index;
    }

    public void setLastRecoveryEndTick(long tick) {
        this.lastRecoveryEndTick = tick;
    }

    public void setParryCooldownRemaining(int ticks) {
        this.parryCooldownRemaining = ticks;
    }

    public void setDodgeCooldownRemaining(int ticks) {
        this.dodgeCooldownRemaining = ticks;
    }

    public void setCurrentWeaponDataId(Identifier id) {
        this.currentWeaponDataId = id;
    }

    public void setAttackHitConnected(boolean connected) {
        this.attackHitConnected = connected;
    }

    /**
     * Tick down timers. Called every server tick.
     */
    public void tickTimers() {
        if (stateTicksRemaining > 0) {
            stateTicksRemaining--;
        }
        if (parryCooldownRemaining > 0) {
            parryCooldownRemaining--;
        }
        if (dodgeCooldownRemaining > 0) {
            dodgeCooldownRemaining--;
        }
    }

    /**
     * Reset all state to idle defaults.
     */
    public void reset() {
        currentState = CombatState.IDLE;
        stateTicksRemaining = 0;
        comboIndex = 0;
        parryCooldownRemaining = 0;
        dodgeCooldownRemaining = 0;
        attackHitConnected = false;
    }
}
