package combat.wulidam.network.s2c;

import combat.wulidam.SoulsLikeCombat;
import combat.wulidam.combat.ComboAttack;
import combat.wulidam.combat.WeaponData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-client payload that bulk-syncs all WeaponData entries.
 * Sent when a player joins so the client has weapon stats for tooltips,
 * HUD display, and animation timing.
 */
public record WeaponDataSyncS2CPayload(List<WeaponDataEntry> entries) implements CustomPayload {

    public static final Id<WeaponDataSyncS2CPayload> ID =
            new Id<>(Identifier.of(SoulsLikeCombat.MOD_ID, "weapon_data_sync_s2c"));

    public static final PacketCodec<PacketByteBuf, WeaponDataSyncS2CPayload> CODEC =
            PacketCodec.of(
                    WeaponDataSyncS2CPayload::write,
                    WeaponDataSyncS2CPayload::read
            );

    private static void write(WeaponDataSyncS2CPayload payload, PacketByteBuf buf) {
        buf.writeVarInt(payload.entries().size());
        for (WeaponDataEntry entry : payload.entries()) {
            buf.writeIdentifier(entry.id());
            buf.writeFloat(entry.baseDamage());
            buf.writeFloat(entry.attackRange());
            buf.writeVarInt(entry.windUpTicks());
            buf.writeVarInt(entry.activeTicks());
            buf.writeVarInt(entry.recoveryTicks());
            buf.writeVarInt(entry.parryWindowTicks());
            buf.writeVarInt(entry.parryStunTicks());
            buf.writeFloat(entry.parryKnockback());
            buf.writeVarInt(entry.parryFailStunTicks());
            buf.writeVarInt(entry.parryCooldownTicks());
            buf.writeVarInt(entry.dodgeTicks());
            buf.writeVarInt(entry.dodgeIFrames());
            buf.writeFloat(entry.dodgeDistance());
            buf.writeVarInt(entry.dodgeCooldownTicks());
            buf.writeVarInt(entry.hitStunTicks());
            buf.writeVarInt(entry.interruptStunTicks());
            buf.writeVarInt(entry.comboCount());
            buf.writeVarInt(entry.comboWindowTicks());

            // Write combo attacks
            buf.writeVarInt(entry.comboAttacks().size());
            for (ComboAttack ca : entry.comboAttacks()) {
                buf.writeFloat(ca.damageMultiplier());
                buf.writeFloat(ca.rangeMultiplier());
                buf.writeVarInt(ca.windUpTicks());
                buf.writeVarInt(ca.activeTicks());
            }
        }
    }

    private static WeaponDataSyncS2CPayload read(PacketByteBuf buf) {
        int count = buf.readVarInt();
        List<WeaponDataEntry> entries = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Identifier id = buf.readIdentifier();
            float baseDamage = buf.readFloat();
            float attackRange = buf.readFloat();
            int windUpTicks = buf.readVarInt();
            int activeTicks = buf.readVarInt();
            int recoveryTicks = buf.readVarInt();
            int parryWindowTicks = buf.readVarInt();
            int parryStunTicks = buf.readVarInt();
            float parryKnockback = buf.readFloat();
            int parryFailStunTicks = buf.readVarInt();
            int parryCooldownTicks = buf.readVarInt();
            int dodgeTicks = buf.readVarInt();
            int dodgeIFrames = buf.readVarInt();
            float dodgeDistance = buf.readFloat();
            int dodgeCooldownTicks = buf.readVarInt();
            int hitStunTicks = buf.readVarInt();
            int interruptStunTicks = buf.readVarInt();
            int comboCount = buf.readVarInt();
            int comboWindowTicks = buf.readVarInt();

            int comboAttackCount = buf.readVarInt();
            List<ComboAttack> comboAttacks = new ArrayList<>(comboAttackCount);
            for (int j = 0; j < comboAttackCount; j++) {
                comboAttacks.add(new ComboAttack(
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readVarInt(),
                        buf.readVarInt()
                ));
            }

            entries.add(new WeaponDataEntry(
                    id, baseDamage, attackRange, windUpTicks, activeTicks, recoveryTicks,
                    parryWindowTicks, parryStunTicks, parryKnockback, parryFailStunTicks,
                    parryCooldownTicks, dodgeTicks, dodgeIFrames, dodgeDistance,
                    dodgeCooldownTicks, hitStunTicks, interruptStunTicks, comboCount,
                    comboWindowTicks, comboAttacks
            ));
        }

        return new WeaponDataSyncS2CPayload(entries);
    }

    /**
     * Convert a WeaponData record to a serializable entry.
     */
    public static WeaponDataEntry fromWeaponData(WeaponData data) {
        return new WeaponDataEntry(
                data.id(), data.baseDamage(), data.attackRange(),
                data.windUpTicks(), data.activeTicks(), data.recoveryTicks(),
                data.parryWindowTicks(), data.parryStunTicks(), data.parryKnockback(),
                data.parryFailStunTicks(), data.parryCooldownTicks(),
                data.dodgeTicks(), data.dodgeIFrames(), data.dodgeDistance(),
                data.dodgeCooldownTicks(), data.hitStunTicks(), data.interruptStunTicks(),
                data.comboCount(), data.comboWindowTicks(),
                data.comboAttacks() != null ? data.comboAttacks() : List.of()
        );
    }

    /**
     * Convert a serializable entry back into a WeaponData record.
     */
    public static WeaponData toWeaponData(WeaponDataEntry entry) {
        return new WeaponData(
                entry.id(), entry.baseDamage(), entry.attackRange(),
                entry.windUpTicks(), entry.activeTicks(), entry.recoveryTicks(),
                entry.parryWindowTicks(), entry.parryStunTicks(), entry.parryKnockback(),
                entry.parryFailStunTicks(), entry.parryCooldownTicks(),
                entry.dodgeTicks(), entry.dodgeIFrames(), entry.dodgeDistance(),
                entry.dodgeCooldownTicks(), entry.hitStunTicks(), entry.interruptStunTicks(),
                entry.comboCount(), entry.comboWindowTicks(), entry.comboAttacks()
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /**
     * Flat data carrier mirroring all WeaponData fields for network serialization.
     */
    public record WeaponDataEntry(
            Identifier id,
            float baseDamage,
            float attackRange,
            int windUpTicks,
            int activeTicks,
            int recoveryTicks,
            int parryWindowTicks,
            int parryStunTicks,
            float parryKnockback,
            int parryFailStunTicks,
            int parryCooldownTicks,
            int dodgeTicks,
            int dodgeIFrames,
            float dodgeDistance,
            int dodgeCooldownTicks,
            int hitStunTicks,
            int interruptStunTicks,
            int comboCount,
            int comboWindowTicks,
            List<ComboAttack> comboAttacks
    ) {}
}
