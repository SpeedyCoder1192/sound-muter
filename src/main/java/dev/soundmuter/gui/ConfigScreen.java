package dev.soundmuter.gui;

import dev.soundmuter.SoundMuter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
//? if >=1.21 {
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.CommonColors;
*///?}

public class ConfigScreen extends Screen {
	private static final int HEADER_HEIGHT = 8 + 9 + 4 + 20 + 4;
	private static final int FOOTER_HEIGHT = 33;

	private final @Nullable Screen parent;
	//? if >=1.21
	private HeaderAndFooterLayout layout;
	private EditBox search;
	private SoundList list;

	public ConfigScreen(@Nullable Screen parent) {
		super(Component.translatable("soundmuter.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		Component searchText = Component.translatable("soundmuter.search");
		Component resetText = Component.translatable("soundmuter.reset_all");
		//? if >=1.21 {
		layout = new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);
		LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(4));
		header.defaultCellSetting().alignHorizontallyCenter();
		header.addChild(new StringWidget(title, font));
		search = header.addChild(new EditBox(font, 0, 0, 200, 20, search, searchText));
		list = layout.addToContents(new SoundList(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight()));

		LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(resetText, button -> confirmReset()).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).build());

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
		//?} else {
		/*// 1.20.1 lists aren't layout elements yet, so place everything by hand
		search = addRenderableWidget(new EditBox(font, width / 2 - 100, 8 + 9 + 4, 200, 20, search, searchText));
		list = addWidget(new SoundList(minecraft, width, height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT));
		int buttonY = height - FOOTER_HEIGHT + 7;
		addRenderableWidget(Button.builder(resetText, button -> confirmReset()).bounds(width / 2 - 154, buttonY, 150, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).bounds(width / 2 + 4, buttonY, 150, 20).build());
		setInitialFocus(search);
		*///?}

		search.setHint(searchText.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		search.setResponder(text -> list.setFilter(text));
		list.setFilter(search.getValue());
	}

	//? if >=1.21 {
	@Override
	protected void setInitialFocus() {
		setInitialFocus(search);
	}

	@Override
	protected void repositionElements() {
		layout.arrangeElements();
		list.updateSize(width, layout);
	}
	//?} else {
	/*@Override
	public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		renderBackground(graphics);
		list.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(font, title, width / 2, 8, CommonColors.WHITE);
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}
	*///?}

	private void confirmReset() {
		minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				SoundMuter.resetAll();
				list.setFilter(search.getValue());
			}
			minecraft.gui.setScreen(this);
		}, Component.translatable("soundmuter.reset_all.title"), Component.translatable("soundmuter.reset_all.message")));
	}

	@Override
	public void removed() {
		SoundMuter.config().save();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
