package combat.wulidam.network.c2s;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server payload sent when the player presses the parry keybind.
 * Contains no data — the server validates and transitions state.
 */
// packet that says the player pressed parry
public record ParryC2SPayload() implements CustomPayload {

    public static final Id<ParryC2SPayload> ID =
            new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "parry_c2s"));

    public static final PacketCodec<PacketByteBuf, ParryC2SPayload> CODEC =
            PacketCodec.unit(new ParryC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
