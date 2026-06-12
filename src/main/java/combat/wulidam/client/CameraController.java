package combat.wulidam.client;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Simple client-side camera controller: supports an over-shoulder offset and a decaying screen shake.
 * This class only computes offsets — applying them to the real Camera is done by a mixin so the
 * combat math (raycasts, hit detection) remains unchanged.
 */
public class CameraController {
    private static final Vec3d ZERO = Vec3d.ZERO;

    // shoulder offset (meters)
    private static double shoulderDistance = 2.0; // increased for debugging to make offset obvious
    private static double shoulderHeight = 0.5;
    private static boolean shoulderRight = true; // look over right shoulder by default

    // shake state
    private static double shakeAmplitude = 0.0; // world units for position
    private static double shakeYaw = 0.0; // degrees
    private static int shakeTicksRemaining = 0;
    private static double shakeFrequency = 20.0; // Hz-ish

    // computed each tick
    private static Vec3d positionOffset = ZERO;
    private static double yawOffset = 0.0;
    private static double pitchOffset = 0.0;

    // orbit yaw (degrees) applied to shoulder calculation so user can rotate camera around player
    private static double orbitYawTargetDegrees = 0.0; // target angle
    private static double orbitYawDegrees = 0.0; // current (smoothed)

    // smoothing factor (0-1] per tick; higher = snappier. 0.2 is fairly smooth.
    private static double orbitSmoothing = 0.22;

    public static void init(MinecraftClient client) {
        // reserved for future hooks
    }

    /**
     * Start a camera shake. amplitude = world-space meters (0.05-0.6 range is good),
     * yawDegrees = small angular component (0.5-6.0), ticks = duration in game ticks (20 ticks = 1s).
     */
    public static void startShake(double amplitude, double yawDegrees, int ticks) {
        shakeAmplitude = Math.max(shakeAmplitude, amplitude);
        shakeYaw = Math.max(shakeYaw, yawDegrees);
        shakeTicksRemaining = Math.max(shakeTicksRemaining, ticks);
    }

    public static void setShoulder(boolean right, double distance, double height) {
        shoulderRight = right;
        shoulderDistance = distance;
        shoulderHeight = height;
    }

    public static void rotateOrbitYaw(double deltaDegrees) {
        orbitYawTargetDegrees = (orbitYawTargetDegrees + deltaDegrees) % 360.0;
        SoulsLikeCombat.LOGGER.debug("CameraController.rotateOrbitYaw -> target={} (delta={})", orbitYawTargetDegrees, deltaDegrees);
    }

    public static double getOrbitYawDegrees() { return orbitYawDegrees; }

    public static boolean isShoulderRight() { return shoulderRight; }

    public static void toggleShoulderSide() { shoulderRight = !shoulderRight; }
    public static void tick(MinecraftClient client, float tickDelta) {
        // compute shoulder offset in world-space using player's rotation
        if (client.player == null) {
            positionOffset = ZERO;
            yawOffset = 0.0;
            pitchOffset = 0.0;
            return;
        }

        // If in first-person, keep offset zero so position changes don't affect view
        try {
            boolean third = isThirdPerson(client);
            // debug: log third-person detection (INFO to ensure visibility)
            SoulsLikeCombat.LOGGER.info("CameraController.isThirdPerson = {}", third);
            if (!third) {
                positionOffset = ZERO;
                yawOffset = 0.0;
                pitchOffset = 0.0;
                return;
            }
        } catch (Throwable t) {
            SoulsLikeCombat.LOGGER.info("CameraController isThirdPerson check failed: {}", t.toString());
            // Reflection failed; assume first-person to avoid applying shoulder offsets incorrectly
            positionOffset = ZERO;
            yawOffset = 0.0;
            pitchOffset = 0.0;
            return;
        }

        // Smooth orbit yaw
        orbitYawDegrees += (orbitYawTargetDegrees - orbitYawDegrees) * orbitSmoothing;
        double yawRad = Math.toRadians(client.player.getYaw(1.0f) + orbitYawDegrees);
        double rightAngle = yawRad + (shoulderRight ? Math.PI/2.0 : -Math.PI/2.0);
        double dx = Math.cos(rightAngle) * shoulderDistance;
        double dz = Math.sin(rightAngle) * shoulderDistance;
        double dy = shoulderHeight;

        Vec3d shoulder = new Vec3d(dx, dy, dz);

        // handle shake
        if (shakeTicksRemaining > 0) {
            double t = (double) shakeTicksRemaining - tickDelta;
            double life = Math.max(1.0, (double)shakeTicksRemaining);
            double decay = Math.exp(- ( (double)(20 - Math.min(20, life)) / 10.0));
            // simple procedural noise using sines at different frequencies
            double time = System.nanoTime() / 1e9;
            double n1 = Math.sin(time * shakeFrequency * 1.0) * 0.5 + Math.sin(time * shakeFrequency * 1.7) * 0.5;
            double n2 = Math.cos(time * shakeFrequency * 1.3) * 0.5 + Math.cos(time * shakeFrequency * 2.1) * 0.5;

            double posAmp = shakeAmplitude * (shakeTicksRemaining /  (double) Math.max(1, life)) * decay;
            double yawAmp = shakeYaw * (shakeTicksRemaining / (double) Math.max(1, life)) * decay;

            Vec3d jitter = new Vec3d(n1 * posAmp, n2 * posAmp * 0.6, n2 * posAmp);
            positionOffset = shoulder.add(jitter);

            yawOffset = n1 * yawAmp;
            pitchOffset = n2 * (yawAmp * 0.35);

            shakeTicksRemaining--;
            if (shakeTicksRemaining <= 0) {
                shakeAmplitude = 0.0;
                shakeYaw = 0.0;
            }
        } else {
            positionOffset = shoulder;
            yawOffset = 0.0;
            pitchOffset = 0.0;
        }
    }

    public static Vec3d getPositionOffset() {
        return positionOffset;
    }

    public static double getYawOffset() { return yawOffset; }
    public static double getPitchOffset() { return pitchOffset; }

    /**
     * Attempt to detect whether the game is currently in third-person using reflection so
     * this code compiles across mappings where the Perspective class may not exist.
     */
    public static boolean isThirdPerson(MinecraftClient client) {
        try {
            Object options = client.options;
            if (options == null) return false;

            // Try method getPerspective()
            try {
                java.lang.reflect.Method m = options.getClass().getMethod("getPerspective");
                Object val = m.invoke(options);
                if (val == null) return false;
                String s = val.toString().toLowerCase();
                if (s.contains("first")) return false;
                return true;
            } catch (NoSuchMethodException ignored) {}

            // Try field perspective
            try {
                java.lang.reflect.Field f = options.getClass().getField("perspective");
                Object val = f.get(options);
                if (val instanceof Integer) {
                    return ((Integer) val) != 0; // 0 == first person
                }
                if (val != null) {
                    String s = val.toString().toLowerCase();
                    return !s.contains("first");
                }
            } catch (NoSuchFieldException ignored) {}

            // Fallback: assume first-person (safe default)
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
