plugins {
	id("net.neoforged.moddev")
}

val mc = stonecutter.current.version
val modId = property("mod.id") as String
val javaVersion = if (stonecutter.current.parsed >= "26.1") 25 else 21

version = "${property("mod.version")}+$mc-neoforge"
group = property("mod.group") as String
base.archivesName = modId

sourceSets.main {
	java.exclude("**/platform/fabric/**")
}

neoForge {
	version = property("deps.neoforge") as String

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
		}
	}

	mods {
		register(modId) {
			sourceSet(sourceSets.main.get())
		}
	}
}

java {
	withSourcesJar()
	toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

tasks.processResources {
	exclude("fabric.mod.json")
	val props = mapOf(
		"version" to project.version,
		"minecraft" to project.property("mod.mc_neoforge"),
		"java" to javaVersion
	)
	inputs.properties(props)
	filesMatching(listOf("META-INF/neoforge.mods.toml", "$modId.mixins.json")) { expand(props) }
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

tasks.jar {
	from(rootProject.file("LICENSE")) {
		rename { "${it}_$modId" }
	}
}

tasks.register<Copy>("collectJar") {
	group = "build"
	from(tasks.jar.map { it.archiveFile })
	into(rootProject.layout.buildDirectory.dir("libs"))
}
