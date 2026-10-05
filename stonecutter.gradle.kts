plugins {
	id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-fabric"

stonecutter parameters {
	constants.match(current.project.substringAfterLast('-'), "fabric", "neoforge")

	// one-line renames between versions, the source is written against the newest names.
	// anything bigger uses //? comments in the code
	replacements {
		string(current.parsed >= "1.21.11") {
			replace("ResourceLocation", "Identifier")
			replace(".getLocation()", ".getIdentifier()")
			replace("import net.minecraft.Util;", "import net.minecraft.util.Util;")
			replace("import org.jetbrains.annotations.Nullable;", "import org.jspecify.annotations.Nullable;")
		}
		string(current.parsed >= "26.1") {
			replace("import net.minecraft.client.gui.GuiGraphics;", "import net.minecraft.client.gui.GuiGraphicsExtractor;")
			replace("GuiGraphics graphics", "GuiGraphicsExtractor graphics")
			replace("void renderContent(", "void extractContent(")
			replace(".render(graphics,", ".extractRenderState(graphics,")
			replace("graphics.drawString(", "graphics.text(")
			replace("graphics.drawCenteredString(", "graphics.centeredText(")
			replace("keybinding.v1.KeyBindingHelper", "keymapping.v1.KeyMappingHelper")
			replace("KeyBindingHelper.registerKeyBinding(", "KeyMappingHelper.registerKeyMapping(")
		}
		string(current.parsed >= "26.2") {
			replace("minecraft.setScreen(", "minecraft.gui.setScreen(")
		}
	}
}

tasks.register("buildAll") {
	group = "build"
	description = "Builds every version and copies the jars to build/libs/"
	stonecutter.versions.forEach { dependsOn(":${it.project}:collectJar") }
}
