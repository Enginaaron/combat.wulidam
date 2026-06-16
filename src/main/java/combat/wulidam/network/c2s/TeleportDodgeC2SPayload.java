package combat.wulidam.network.c2s;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Client-to-server payload for teleport-dodge requests. Contains a small offset
 * vector (usually horizontal) that the server will apply to the player's position.
 */
// packet for blink dodging a small distance
public record TeleportDodgeC2SPayload(double dirX, double dirY, double dirZ) implements CustomPayload {

    public static final Id<TeleportDodgeC2SPayload> ID =
            new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "teleport_dodge_c2s"));

    public static final PacketCodec<PacketByteBuf, TeleportDodgeC2SPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeDouble(payload.dirX());
                        buf.writeDouble(payload.dirY());
                        buf.writeDouble(payload.dirZ());
                    },
                    buf -> new TeleportDodgeC2SPayload(buf.readDouble(), buf.readDouble(), buf.readDouble())
            );

    public TeleportDodgeC2SPayload(Vec3d direction) {
        this(direction.x, direction.y, direction.z);
    }

    public Vec3d getDirection() {
        return new Vec3d(dirX, dirY, dirZ);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
