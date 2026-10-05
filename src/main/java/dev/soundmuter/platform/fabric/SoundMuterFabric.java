package dev.soundmuter.platform.fabric;

import dev.soundmuter.SoundMuter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
//? if >=1.21.10
import net.minecraft.client.KeyMapping;

public class SoundMuterFabric implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		SoundMuter.init(FabricLoader.getInstance().getConfigDir());
		//? if >=1.21.10
		KeyMapping.Category.register(SoundMuter.KEY_CATEGORY.id());
		KeyMappingHelper.registerKeyMapping(SoundMuter.OPEN_KEY);
		KeyMappingHelper.registerKeyMapping(SoundMuter.MUTE_LAST_KEY);
		ClientTickEvents.END_CLIENT_TICK.register(SoundMuter::onTick);
	}
}
