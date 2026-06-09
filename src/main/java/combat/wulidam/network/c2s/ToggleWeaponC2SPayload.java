package combat.wulidam.network.c2s;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server payload sent when the player presses the toggle-weapon keybind.
 * Empty payload: server derives everything from the player's current state.
 */
public record ToggleWeaponC2SPayload() implements CustomPayload {

    public static final Id<ToggleWeaponC2SPayload> ID =
            new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "toggle_weapon_c2s"));

    public static final PacketCodec<PacketByteBuf, ToggleWeaponC2SPayload> CODEC =
            PacketCodec.unit(new ToggleWeaponC2SPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
