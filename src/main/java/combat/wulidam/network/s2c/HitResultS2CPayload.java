package combat.wulidam.network.s2c;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Server-to-client payload carrying the result of a combat interaction.
 * Used by the client for screen shake, hit flash, sounds, and damage numbers.
 *
 * Hit types:
 *   0 = normal hit
 *   1 = parried (attacker was parried)
 *   2 = dodged (attack missed due to i-frames)
 *   3 = interrupted (target was mid-attack)
 */
public record HitResultS2CPayload(
        UUID attackerUuid,
        UUID targetUuid,
        float damageDealt,
        int hitType
) implements CustomPayload {

    // Hit type constants
    public static final int HIT_NORMAL = 0;
    public static final int HIT_PARRIED = 1;
    public static final int HIT_DODGED = 2;
    public static final int HIT_INTERRUPTED = 3;

    public static final Id<HitResultS2CPayload> ID =
            new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "hit_result_s2c"));

    public static final PacketCodec<PacketByteBuf, HitResultS2CPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeUuid(payload.attackerUuid());
                        buf.writeUuid(payload.targetUuid());
                        buf.writeFloat(payload.damageDealt());
                        buf.writeVarInt(payload.hitType());
                    },
                    buf -> new HitResultS2CPayload(
                            buf.readUuid(),
                            buf.readUuid(),
                            buf.readFloat(),
                            buf.readVarInt()
                    )
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
