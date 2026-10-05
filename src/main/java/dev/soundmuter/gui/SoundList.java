package dev.soundmuter.gui;

import dev.soundmuter.SoundMuter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;

public class SoundList extends ContainerObjectSelectionList<SoundList.Entry> {
	private static final int SOUND_HEIGHT = 24;
	private static final int HEADER_HEIGHT = 16;

	public SoundList(Minecraft minecraft, int width, int height, int y) {
		//? if >=1.21 {
		super(minecraft, width, height, y, SOUND_HEIGHT);
		//?} else
		/*super(minecraft, width, height, y, y + height, SOUND_HEIGHT);*/
	}

	public void setFilter(String filter) {
		String query = filter.trim().toLowerCase(Locale.ROOT);
		clearEntries();

		List<Identifier> recent = SoundMuter.recent().newestFirst();
		if (recent.isEmpty() && query.isEmpty()) {
			addSection("soundmuter.section.recent");
			add(new TextEntry(Component.translatable("soundmuter.nothing_heard").withStyle(ChatFormatting.GRAY)), HEADER_HEIGHT);
		} else {
			addSounds("soundmuter.section.recent", recent, query);
		}

		addSounds("soundmuter.section.changed", sorted(SoundMuter.config().getChanged()), query);

		Map<String, List<Identifier>> groups = new TreeMap<>();
		for (Identifier id : sorted(minecraft.getSoundManager().getAvailableSounds())) {
			if (matches(id, query)) {
				groups.computeIfAbsent(group(id), group -> new ArrayList<>()).add(id);
			}
		}
		if (!groups.isEmpty()) {
			addSection("soundmuter.section.all");
			groups.forEach((group, ids) -> {
				add(new TextEntry(Component.literal(group).withStyle(ChatFormatting.GRAY)), HEADER_HEIGHT);
				ids.forEach(id -> add(new SoundEntry(id), SOUND_HEIGHT));
			});
		}

		setScrollAmount(0);
	}

	private void addSounds(String titleKey, List<Identifier> ids, String query) {
		List<Identifier> matching = ids.stream().filter(id -> matches(id, query)).toList();
		if (!matching.isEmpty()) {
			addSection(titleKey);
			matching.forEach(id -> add(new SoundEntry(id), SOUND_HEIGHT));
		}
	}

	private void addSection(String titleKey) {
		add(new TextEntry(Component.translatable(titleKey).withStyle(ChatFormatting.BOLD, ChatFormatting.UNDERLINE)), HEADER_HEIGHT + 4);
	}

	private void add(Entry entry, int height) {
		//? if >=1.21.10 {
		addEntry(entry, height);
		//?} else {
		/*// older lists give every row the same height
		addEntry(entry);
		*///?}
	}

	private static boolean matches(Identifier id, String query) {
		return query.isEmpty()
			|| id.toString().contains(query)
			|| SoundMuter.displayName(id).getString().toLowerCase(Locale.ROOT).contains(query);
	}

	// "entity.villager.ambient" -> "entity"
	private static String group(Identifier id) {
		String path = id.getPath();
		int dot = path.indexOf('.');
		return dot < 0 ? path : path.substring(0, dot);
	}

	private static List<Identifier> sorted(Collection<Identifier> ids) {
		List<Identifier> list = new ArrayList<>(ids);
		list.sort(null);
		return list;
	}

	// the same sound can be listed in several sections, keep all of its rows in sync
	private void refresh(Identifier id) {
		for (Entry entry : children()) {
			if (entry instanceof SoundEntry sound && sound.id.equals(id)) {
				sound.refresh();
			}
		}
	}

	@Override
	public int getRowWidth() {
		return 340;
	}

	//? if <1.21 {
	/*// 1.20.1 puts the scrollbar at a fixed offset meant for 220 wide rows
	@Override
	protected int getScrollbarPosition() {
		return getRowLeft() + getRowWidth() + 4;
	}
	*///?}

	public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
		abstract void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY, float a);

		//? if >=1.21.10 {
		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
			draw(graphics, getContentX(), getContentY(), getContentWidth(), getContentHeight(), mouseX, mouseY, a);
		}
		//?} else {
		/*@Override
		public void render(GuiGraphicsExtractor graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float a) {
			draw(graphics, x, y, width, height, mouseX, mouseY, a);
		}
		*///?}
	}

	class TextEntry extends Entry {
		private final Component text;

		TextEntry(Component text) {
			this.text = text;
		}

		@Override
		void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY, float a) {
			graphics.centeredText(minecraft.font, text, x + width / 2, y + height - 9, CommonColors.WHITE);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of();
		}
	}

	class SoundEntry extends Entry {
		private final Identifier id;
		private final String name;
		private final String idText;
		private final Button previewButton;
		private final Button muteButton;
		private final VolumeSlider slider;

		SoundEntry(Identifier id) {
			this.id = id;
			this.name = SoundMuter.displayName(id).getString();
			// vanilla ids are the long ones, and the namespace adds nothing for them
			this.idText = id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? id.getPath() : id.toString();
			previewButton = Button.builder(Component.literal("▶"), button -> SoundMuter.preview(id))
				.size(20, 20)
				.tooltip(Tooltip.create(Component.translatable("soundmuter.preview")))
				.build();
			muteButton = Button.builder(Component.empty(), button -> {
				SoundMuter.setVolume(id, SoundMuter.config().isMuted(id) ? 1f : 0f);
				SoundList.this.refresh(id);
			}).size(50, 20).build();
			slider = new VolumeSlider();
			refresh();
		}

		void refresh() {
			boolean muted = SoundMuter.config().isMuted(id);
			muteButton.setMessage(Component.translatable(muted ? "soundmuter.unmute" : "soundmuter.mute"));
			slider.sync();
		}

		@Override
		void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY, float a) {
			int buttonY = y + height / 2 - 10;
			slider.setPosition(x + width - slider.getWidth(), buttonY);
			muteButton.setPosition(slider.getX() - 4 - muteButton.getWidth(), buttonY);
			previewButton.setPosition(muteButton.getX() - 4 - previewButton.getWidth(), buttonY);

			Font font = minecraft.font;
			int textWidth = previewButton.getX() - 6 - x;
			// no subtitle, the id is all we have
			if (name.equals(id.toString())) {
				graphics.text(font, font.plainSubstrByWidth(idText, textWidth), x, y + height / 2 - 4, CommonColors.WHITE);
			} else {
				graphics.text(font, font.plainSubstrByWidth(name, textWidth), x, y + 1, CommonColors.WHITE);
				graphics.text(font, font.plainSubstrByWidth(idText, textWidth), x, y + 11, CommonColors.GRAY);
			}

			previewButton.extractRenderState(graphics, mouseX, mouseY, a);
			muteButton.extractRenderState(graphics, mouseX, mouseY, a);
			slider.extractRenderState(graphics, mouseX, mouseY, a);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(previewButton, muteButton, slider);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(previewButton, muteButton, slider);
		}

		class VolumeSlider extends AbstractSliderButton {
			VolumeSlider() {
				super(0, 0, 90, 20, Component.empty(), SoundMuter.config().getVolume(id));
			}

			void sync() {
				value = SoundMuter.config().getVolume(id);
				updateMessage();
			}

			@Override
			protected void updateMessage() {
				setMessage(value <= 0
					? Component.translatable("soundmuter.muted")
					: Component.literal(Math.round(value * 100) + "%"));
			}

			@Override
			protected void applyValue() {
				SoundMuter.setVolume(id, Math.round(value * 100) / 100f);
				SoundList.this.refresh(id);
			}
		}
	}
}
