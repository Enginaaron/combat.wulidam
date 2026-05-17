package combat.wulidam.network.c2s;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Client-to-server payload sent when the player presses the dodge keybind.
 * Includes the intended movement direction (WASD-relative) so the server
 * can apply dodge velocity in the correct direction.
 */
public record DodgeC2SPayload(double dirX, double dirY, double dirZ) implements CustomPayload {

    public static final Id<DodgeC2SPayload> ID =
            new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "dodge_c2s"));

    public static final PacketCodec<PacketByteBuf, DodgeC2SPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeDouble(payload.dirX());
                        buf.writeDouble(payload.dirY());
                        buf.writeDouble(payload.dirZ());
                    },
                    buf -> new DodgeC2SPayload(buf.readDouble(), buf.readDouble(), buf.readDouble())
            );

    /**
     * Convenience constructor from a Vec3d direction.
     */
    public DodgeC2SPayload(Vec3d direction) {
        this(direction.x, direction.y, direction.z);
    }

    /**
     * @return the dodge direction as a Vec3d.
     */
    public Vec3d getDirection() {
        return new Vec3d(dirX, dirY, dirZ);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
