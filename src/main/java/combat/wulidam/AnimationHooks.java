package combat.wulidam;

/**
 * Simple client-visible hook for animations/effects. Place Blender animation
 * integration here. Kept in the base package to avoid creating new directories.
 */
// little spot for client animations so the other code can call it
public class AnimationHooks {
    public static void playBlenderDodge() {
        // TODO: implement Blender animation trigger
        SoulsLikeCombat.LOGGER.debug("AnimationHooks.playBlenderDodge called");
    }

    public static void playDodgeBump() {
        // Play a small bump animation/effect when teleport-dodge is blocked by collision
        SoulsLikeCombat.LOGGER.debug("AnimationHooks.playDodgeBump called");
    }
}
