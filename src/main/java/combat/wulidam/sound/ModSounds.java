package combat.wulidam.sound;

import combat.wulidam.SoulsLikeCombat;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent PARRY_SUCCESS = register("parry_success");
    public static final SoundEvent POSTURE_BREAK = register("posture_break");
    public static final SoundEvent DODGE = register("dodge");
    public static final SoundEvent BLOCK = register("block");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.of(SoulsLikeCombat.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void initialize() {
        // Just to trigger class loading and static field initialization
    }
}
