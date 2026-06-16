package combat.wulidam.network.c2s;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server payload sent when the player switches hotbar slot and may want to reassemble.
 * Contains the hotbar slot indices where the client found the sword and shield (-1 if not found).
 */
// packet asking server to put sword and shield back together
public record RequestReassembleC2SPayload(int swordSlot, int shieldSlot) implements CustomPayload {
    public static final Id<RequestReassembleC2SPayload> ID =
            new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "request_reassemble_c2s"));

    public static final PacketCodec<PacketByteBuf, RequestReassembleC2SPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeInt(payload.swordSlot());
                        buf.writeInt(payload.shieldSlot());
                    },
                    buf -> new RequestReassembleC2SPayload(buf.readInt(), buf.readInt())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
