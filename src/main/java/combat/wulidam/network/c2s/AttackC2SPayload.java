package combat.wulidam.network.c2s;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server payload sent when the player presses the attack keybind.
 * Contains no data — the server derives everything from the player's current state.
 */
// packet that says the client wants to attack
public record AttackC2SPayload() implements CustomPayload {

    public static final Id<AttackC2SPayload> ID =
            new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "attack_c2s"));

    public static final PacketCodec<PacketByteBuf, AttackC2SPayload> CODEC =
            PacketCodec.unit(new AttackC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
