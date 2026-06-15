package combat.wulidam;

import combat.wulidam.combat.WeaponRegistry;
import combat.wulidam.event.CombatTickHandler;
import combat.wulidam.item.ModItems;
import combat.wulidam.network.ModPayloads;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoulsLikeCombat implements ModInitializer {
	public static final String MOD_ID = "soulslikecombat";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing SoulsLikeCombat...");

		// Register custom weapon items and creative tab entries
		ModItems.initialize();

		// Register sounds
		combat.wulidam.sound.ModSounds.initialize();

		// Register weapon data resource reload listener
		WeaponRegistry.register();

		// Register networking payload types (must happen before receivers)
		ModPayloads.registerPayloadTypes();

		// Register server-side C2S packet receivers
		ModPayloads.registerServerReceivers();

		// Register server tick handler for combat state machine
		CombatTickHandler.register();

		LOGGER.info("SoulsLikeCombat initialized successfully!");
	}
}