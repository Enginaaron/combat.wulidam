package combat.wulidam;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;

/**
 * Server-side tracker for players who had their SwordAndShield split.
 * Stores the ItemStack reference that was in the player's main hand right
 * after splitting; reassembly occurs only when the player's current main
 * hand ItemStack reference differs (i.e., they switched hotbar slot).
 */
// remembers who split the sword and shield so it can be put back
public final class SplitTracker {
    // Map of player UUID -> the ItemStack object reference present in main hand after split
    private static final Map<UUID, ItemStack> SAVED_STACK = new ConcurrentHashMap<>();

    private SplitTracker() {}

    public static void markSplit(UUID playerUuid, ItemStack mainHandStack) {
        SAVED_STACK.put(playerUuid, mainHandStack);
    }

    public static boolean isMarked(UUID playerUuid) {
        return SAVED_STACK.containsKey(playerUuid);
    }

    public static ItemStack getSavedStack(UUID playerUuid) {
        return SAVED_STACK.get(playerUuid);
    }

    public static void clearSplit(UUID playerUuid) {
        SAVED_STACK.remove(playerUuid);
    }
}
