package combat.wulidam.network.s2c;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-client notification that the server executed a teleport-dodge.
 * Client-side can use this to play animations/effects. Empty payload for now.
 */
// server tells client if the teleport dodge worked or bonked
public record TeleportDodgeS2CPayload(boolean bumped) implements CustomPayload {

    public static final Id<TeleportDodgeS2CPayload> ID =
            new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "teleport_dodge_s2c"));

    public static final PacketCodec<PacketByteBuf, TeleportDodgeS2CPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeBoolean(payload.bumped()),
                    buf -> new TeleportDodgeS2CPayload(buf.readBoolean())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
