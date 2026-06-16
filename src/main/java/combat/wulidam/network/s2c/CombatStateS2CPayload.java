package combat.wulidam.network.s2c;

import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.combat.CombatState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-client payload that syncs the player's combat state.
 * Sent whenever the player's CombatState changes so the client HUD
 * and animation manager can react.
 */
// server sends this so the client knows hp-ish bars and combat state
public record CombatStateS2CPayload(
        int stateOrdinal,
        int stateTicksRemaining,
        int comboIndex,
        int parryCooldown,
        int dodgeCooldown,
        float stamina,
        float maxStamina,
        float posture,
        float maxPosture,
        int ventCooldown
) implements CustomPayload {

    public static final Id<CombatStateS2CPayload> ID =
            new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "combat_state_s2c"));

    public static final PacketCodec<PacketByteBuf, CombatStateS2CPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeVarInt(payload.stateOrdinal());
                        buf.writeVarInt(payload.stateTicksRemaining());
                        buf.writeVarInt(payload.comboIndex());
                        buf.writeVarInt(payload.parryCooldown());
                        buf.writeVarInt(payload.dodgeCooldown());
                        buf.writeFloat(payload.stamina());
                        buf.writeFloat(payload.maxStamina());
                        buf.writeFloat(payload.posture());
                        buf.writeFloat(payload.maxPosture());
                        buf.writeVarInt(payload.ventCooldown());
                    },
                    buf -> new CombatStateS2CPayload(
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readVarInt()
                    )
            );

    /**
     * Convenience constructor from combat data fields.
     */
    public CombatStateS2CPayload(CombatState state, int ticksRemaining, int comboIndex,
                                  int parryCooldown, int dodgeCooldown, float stamina, float maxStamina,
                                  float posture, float maxPosture, int ventCooldown) {
        this(state.ordinal(), ticksRemaining, comboIndex, parryCooldown, dodgeCooldown, stamina, maxStamina, posture, maxPosture, ventCooldown);
    }

    /**
     * @return the CombatState enum value from the ordinal.
     */
    public CombatState getState() {
        CombatState[] values = CombatState.values();
        if (stateOrdinal >= 0 && stateOrdinal < values.length) {
            return values[stateOrdinal];
        }
        return CombatState.IDLE;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
