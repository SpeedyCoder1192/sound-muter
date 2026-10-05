package dev.soundmuter;

import com.mojang.blaze3d.platform.InputConstants;
import dev.soundmuter.core.MuteConfig;
import dev.soundmuter.core.RecentSounds;
import dev.soundmuter.gui.ConfigScreen;
import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SoundMuter {
	public static final String MOD_ID = "soundmuter";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	//? if >=1.21.10 {
	// registered by the loader-specific code, NeoForge wants it done through its own event
	public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
	//?} else
	/*private static final String KEY_CATEGORY = "key.category.soundmuter.main";*/
	public static final KeyMapping OPEN_KEY = new KeyMapping("key.soundmuter.open", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);
	public static final KeyMapping MUTE_LAST_KEY = new KeyMapping("key.soundmuter.mute_last", InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);

	private static final long UNDO_TIME = 5000;
	//? if >=1.21 {
	private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId(UNDO_TIME);
	//?} else
	/*private static final SystemToast.SystemToastIds TOAST = SystemToast.SystemToastIds.PERIODIC_NOTIFICATION;*/

	private static MuteConfig config;
	private static final RecentSounds recent = new RecentSounds();
	private static @Nullable SoundInstance preview;

	private static @Nullable Identifier undoId;
	private static float undoVolume;
	private static long undoUntil;

	private SoundMuter() {
	}

	public static void init(Path configDir) {
		config = new MuteConfig(configDir.resolve(MOD_ID + ".json"));
		config.load();
	}

	public static MuteConfig config() {
		return config;
	}

	public static RecentSounds recent() {
		return recent;
	}

	/** @return true if the sound should be cancelled */
	public static boolean onPlay(SoundInstance sound) {
		if (sound == preview) {
			return false;
		}
		Identifier id = sound.getIdentifier();
		// UI sounds would bury everything else: every button click, and our own toast
		// would end up as the "last sound" for the mute key
		//? if >=1.21.10 {
		if (sound.getSource() != SoundSource.UI) {
		//?} else
		/*if (sound.getSource() != SoundSource.MASTER) {*/
			recent.add(id);
		}
		return config.isMuted(id);
	}

	public static float adjustVolume(SoundInstance sound, float volume) {
		if (sound == preview) {
			return volume;
		}
		return volume * config.getVolume(sound.getIdentifier());
	}

	public static void setVolume(Identifier id, float volume) {
		config.setVolume(id, volume);
		if (volume <= 0f) {
			// non-ticking sounds never re-check their volume, and a music track can run for minutes
			Minecraft.getInstance().getSoundManager().stop(id, null);
		}
	}

	public static void resetAll() {
		config.clear();
		config.save();
		undoId = null;
	}

	public static void preview(Identifier id) {
		preview = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id), 1f, 1f);
		Minecraft.getInstance().getSoundManager().play(preview);
	}

	public static Component displayName(Identifier id) {
		WeighedSoundEvents event = Minecraft.getInstance().getSoundManager().getSoundEvent(id);
		Component subtitle = event == null ? null : event.getSubtitle();
		return subtitle != null ? subtitle : Component.literal(id.toString());
	}

	public static void onTick(Minecraft minecraft) {
		while (OPEN_KEY.consumeClick()) {
			minecraft.gui.setScreen(new ConfigScreen(null));
		}
		while (MUTE_LAST_KEY.consumeClick()) {
			muteLast(minecraft);
		}
	}

	private static void muteLast(Minecraft minecraft) {
		long now = Util.getMillis();
		if (undoId != null && now < undoUntil) {
			setVolume(undoId, undoVolume);
			toast(minecraft, Component.translatable("soundmuter.toast.unmuted", displayName(undoId)), null);
			undoId = null;
			config.save();
			return;
		}

		// muted sounds still show up in the recent list, skip them so the key always mutes something new
		Identifier target = null;
		for (Identifier id : recent.newestFirst()) {
			if (!config.isMuted(id)) {
				target = id;
				break;
			}
		}
		if (target == null) {
			return;
		}

		undoId = target;
		undoVolume = config.getVolume(target);
		undoUntil = now + UNDO_TIME;
		setVolume(target, 0f);
		config.save();
		toast(minecraft, Component.translatable("soundmuter.toast.muted", displayName(target)), Component.translatable("soundmuter.toast.undo"));
	}

	private static void toast(Minecraft minecraft, Component title, @Nullable Component message) {
		//? if >=26.2 {
		SystemToast.addOrUpdate(minecraft.gui.toastManager(), TOAST, title, message);
		//?} elif >=1.21.10 {
		/*SystemToast.addOrUpdate(minecraft.getToastManager(), TOAST, title, message);
		*///?} else {
		/*SystemToast.addOrUpdate(minecraft.getToasts(), TOAST, title, message);
		*///?}
	}
}
