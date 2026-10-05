package dev.soundmuter.platform.neoforge;

import dev.soundmuter.SoundMuter;
import dev.soundmuter.gui.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SoundMuter.MOD_ID, dist = Dist.CLIENT)
public class SoundMuterNeoForge {
	public SoundMuterNeoForge(IEventBus modBus, ModContainer container) {
		SoundMuter.init(FMLPaths.CONFIGDIR.get());
		modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
			//? if >=1.21.10
			event.registerCategory(SoundMuter.KEY_CATEGORY);
			event.register(SoundMuter.OPEN_KEY);
			event.register(SoundMuter.MUTE_LAST_KEY);
		});
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> SoundMuter.onTick(Minecraft.getInstance()));
		container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ConfigScreen(parent));
	}
}
