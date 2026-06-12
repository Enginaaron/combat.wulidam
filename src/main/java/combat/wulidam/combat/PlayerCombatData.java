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

    // --- stamina ---
    private float stamina;
    private float maxStamina;

    // --- posture ---
    private float posture;
    private float maxPosture;
    private int postureImmunityTicks = 0;

    // --- vent ---
    private int ventCooldownRemaining = 0;

    // --- hemorrhage Tracking ---
    private java.util.List<DamageRecord> damageHistory = new java.util.ArrayList<>();
    private static final int HEMORRHAGE_WINDOW = 200; // 10 seconds

    // --- Weapon ---
    private Identifier currentWeaponDataId = null;

    // --- Flags ---
    /** True if an attack in the current active frames has already dealt damage to a target. */
    private boolean attackHitConnected = false;
    /** True once the initial weapon data sync has been sent to this player's client. */
    private boolean initialSync = false;

    // --- shield ---
    private int shieldCooldownRemaining = 0;

    public PlayerCombatData(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.stamina = 100.0f;
        this.maxStamina = 100.0f;
        this.posture = 100.0f;
        this.maxPosture = 100.0f;
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

    public float getStamina() {
        return stamina;
    }

    public float getMaxStamina() {
        return maxStamina;
    }

    public float getPosture() {
        return posture;
    }

    public float getMaxPosture() {
        return maxPosture;
    }

    public int getPostureImmunityTicks() {
        return postureImmunityTicks;
    }

    public int getVentCooldownRemaining() {
        return ventCooldownRemaining;
    }

    public int getShieldCooldownRemaining() {
        return shieldCooldownRemaining;
    }

    public Identifier getCurrentWeaponDataId() {
        return currentWeaponDataId;
    }

    public boolean hasAttackHitConnected() {
        return attackHitConnected;
    }

    public boolean hasInitialSync() {
        return initialSync;
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

    public void setStamina(float stamina) {
        this.stamina = Math.max(0, Math.min(maxStamina, stamina));
    }

    public void setMaxStamina(float maxStamina) {
        this.maxStamina = maxStamina;
        if (this.stamina > maxStamina) {
            this.stamina = maxStamina;
        }
    }

    public void setCurrentWeaponDataId(Identifier id) {
        this.currentWeaponDataId = id;
    }

    public void setAttackHitConnected(boolean connected) {
        this.attackHitConnected = connected;
    }

    public void setInitialSync(boolean synced) {
        this.initialSync = synced;
    }

    public void setPosture(float posture) {
        this.posture = Math.max(0, Math.min(maxPosture, posture));
    }

    public void setPostureImmunityTicks(int ticks) {
        this.postureImmunityTicks = ticks;
    }

    public void setVentCooldownRemaining(int ticks) {
        this.ventCooldownRemaining = ticks;
    }

    public void setShieldCooldownRemaining(int ticks) {
        this.shieldCooldownRemaining = ticks;
    }

    public void addDamageRecord(float amount) {
        damageHistory.add(new DamageRecord(System.currentTimeMillis(), amount));
    }

    public float getRecentDamageTaken() {
        long now = System.currentTimeMillis();
        damageHistory.removeIf(record -> now - record.timestamp > 10000); // 10 seconds
        float total = 0;
        for (DamageRecord record : damageHistory) {
            total += record.amount;
        }
        return total;
    }

    public void resetHemorrhage() {
        damageHistory.clear();
    }

    private record DamageRecord(long timestamp, float amount) {}

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
        if (postureImmunityTicks > 0) {
            postureImmunityTicks--;
        }
        if (ventCooldownRemaining > 0) {
            ventCooldownRemaining--;
        }
        if (shieldCooldownRemaining > 0) {
            shieldCooldownRemaining--;
        }

        // Regenerate stamina (3% per second = 0.15% per tick)
        if (stamina < maxStamina) {
            stamina = Math.min(maxStamina, stamina + (maxStamina * 0.0015f));
        }

        // regenerate posture (1% per second = 0.05% per tick)
        if (posture < maxPosture && currentState != CombatState.STUNNED) {
            posture = Math.min(maxPosture, posture + (maxPosture * 0.0005f));
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
        stamina = maxStamina;
        attackHitConnected = false;
        initialSync = false;
    }
}
