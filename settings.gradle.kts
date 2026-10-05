pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie" }
	}
	plugins {
		id("net.neoforged.moddev") version "2.0.148"
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
	id("dev.kikugie.stonecutter") version "0.9.8"
	id("dev.kikugie.loom-back-compat") version "0.4.2"
}

stonecutter {
	create(rootProject) {
		fun mc(version: String, vararg loaders: String) =
			loaders.forEach { version("$version-$it", version).buildscript = "build.$it.gradle.kts" }

		mc("26.3", "fabric", "neoforge")
		mc("26.2", "fabric", "neoforge")
		mc("26.1.2", "fabric", "neoforge")
		mc("1.21.11", "fabric", "neoforge")
		mc("1.21.10", "fabric", "neoforge")
		mc("1.21.1", "fabric", "neoforge")
		// NeoForge starts at 1.20.2
		mc("1.20.1", "fabric")

		vcsVersion = "26.3-fabric"
	}
}

rootProject.name = "soundmuter"
