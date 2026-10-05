package dev.soundmuter.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.soundmuter.SoundMuter;
import dev.soundmuter.gui.ConfigScreen;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class SoundMuterClientTest implements FabricClientGameTest {
	private static final SoundEvent BELL = SoundEvents.NOTE_BLOCK_BELL.value();
	private static final Identifier BELL_ID = BELL.location();

	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(mc -> SoundMuter.resetAll());

		// recently heard + mixin cancel
		check(playBell(context) == SoundEngine.PlayResult.STARTED, "bell should play while unmuted");
		check(context.computeOnClient(mc -> SoundMuter.recent().newestFirst().contains(BELL_ID)), "bell should be in recently heard");
		context.runOnClient(mc -> SoundMuter.setVolume(BELL_ID, 0f));
		check(playBell(context) == SoundEngine.PlayResult.NOT_STARTED, "muted bell should be cancelled");
		check(context.computeOnClient(mc -> SoundMuter.recent().newestFirst().getFirst().equals(BELL_ID)), "muted bell should still be recorded");

		// preview ignores the mute
		context.runOnClient(mc -> SoundMuter.preview(BELL_ID));
		context.waitTick();
		check(context.computeOnClient(mc -> mc.getSoundManager().isActive(previewInstance())), "preview should play even when muted");

		context.runOnClient(mc -> SoundMuter.setVolume(BELL_ID, 0.5f));
		check(playBell(context) == SoundEngine.PlayResult.STARTED, "bell at 50% should play");
		check(context.computeOnClient(mc -> SoundMuter.adjustVolume(bellInstance(), 0.8f)) == 0.4f, "volume should be scaled");
		context.runOnClient(mc -> SoundMuter.resetAll());

		// GUI
		context.setScreen(() -> new ConfigScreen(null));
		context.waitForScreen(ConfigScreen.class);
		context.takeScreenshot("soundmuter-screen");
		context.getInput().typeChars("bell");
		context.waitTick();
		context.takeScreenshot("soundmuter-search");
		clickWidget(context, "soundmuter.mute");
		context.waitTick();
		check(context.computeOnClient(mc -> SoundMuter.config().isMuted(BELL_ID)), "mute button should mute the first row (bell)");
		context.takeScreenshot("soundmuter-muted");

		context.clickScreenButton("soundmuter.reset_all");
		context.waitForScreen(ConfirmScreen.class);
		context.clickScreenButton("gui.yes");
		context.waitForScreen(ConfigScreen.class);
		check(context.computeOnClient(mc -> SoundMuter.config().getChanged().isEmpty()), "reset all should clear everything");

		context.runOnClient(mc -> SoundMuter.setVolume(BELL_ID, 0.25f));
		context.clickScreenButton("gui.done");
		context.waitForScreen(TitleScreen.class);
		Path file = FabricLoader.getInstance().getConfigDir().resolve("soundmuter.json");
		check(readString(file).contains("\"minecraft:block.note_block.bell\": 0.25"), "closing the screen should save the config");

		// mute-last keybind, in a world because key mappings don't fire on the title screen
		context.runOnClient(mc -> {
			SoundMuter.resetAll();
			//? if >=26.3 {
			SoundMuter.MUTE_LAST_KEY.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_M));
			//?} else
			/*SoundMuter.MUTE_LAST_KEY.setKey(InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_M));*/
			KeyMapping.resetMapping();
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			//? if >=26.2 {
			world.getConnection().waitForChunksRender();
			//?} elif >=26.1 {
			/*world.getClientLevel().waitForChunksRender();
			*///?} else {
			/*world.getClientWorld().waitForChunksRender();
			*///?}
			playBell(context);
			context.getInput().pressKey(SoundMuter.MUTE_LAST_KEY);
			context.waitTick();
			check(context.computeOnClient(mc -> SoundMuter.config().isMuted(BELL_ID)), "mute-last should mute the bell");
			// rendering is slow here, keep the second press well inside the 5s undo window
			context.getInput().pressKey(SoundMuter.MUTE_LAST_KEY);
			context.waitTick();
			check(context.computeOnClient(mc -> !SoundMuter.config().isMuted(BELL_ID)), "second press should undo");

			context.getInput().pressKey(SoundMuter.MUTE_LAST_KEY);
			context.waitTick();
			context.takeScreenshot("soundmuter-toast");
			check(context.computeOnClient(mc -> SoundMuter.config().isMuted(BELL_ID)), "pressing after an undo should mute again");
		}
		context.runOnClient(mc -> SoundMuter.resetAll());
	}

	// clickScreenButton only looks at the screen's direct children, not widgets inside list rows
	private static void clickWidget(ClientGameTestContext context, String translationKey) {
		double[] pos = context.computeOnClient(mc -> {
			//? if >=26.2 {
			AbstractWidget widget = findWidget(mc.gui.screen(), translationKey);
			//?} else
			/*AbstractWidget widget = findWidget(mc.screen, translationKey);*/
			check(widget != null, "no widget '" + translationKey + "' on screen");
			int scale = mc.getWindow().getGuiScale();
			return new double[] {(widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
	}

	private static AbstractWidget findWidget(ContainerEventHandler parent, String translationKey) {
		for (GuiEventListener child : parent.children()) {
			if (child instanceof AbstractWidget widget
				&& widget.getMessage().getContents() instanceof TranslatableContents contents
				&& contents.getKey().equals(translationKey)) {
				return widget;
			}
			if (child instanceof ContainerEventHandler container) {
				AbstractWidget found = findWidget(container, translationKey);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static SoundInstance bellInstance() {
		return SimpleSoundInstance.forLocalAmbience(BELL, 1f, 1f);
	}

	private static SoundEngine.PlayResult playBell(ClientGameTestContext context) {
		return context.computeOnClient(mc -> mc.getSoundManager().play(bellInstance()));
	}

	private static SoundInstance previewInstance() {
		try {
			Field field = SoundMuter.class.getDeclaredField("preview");
			field.setAccessible(true);
			return (SoundInstance) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static String readString(Path file) {
		try {
			return Files.readString(file);
		} catch (Exception e) {
			throw new AssertionError("Couldn't read " + file, e);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
