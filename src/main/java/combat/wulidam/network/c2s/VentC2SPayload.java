package combat.wulidam.network.c2s;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// packet that says the player used vent
public record VentC2SPayload() implements CustomPayload {
    public static final Id<VentC2SPayload> ID = new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "vent_c2s"));
    public static final PacketCodec<PacketByteBuf, VentC2SPayload> CODEC = PacketCodec.unit(new VentC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
