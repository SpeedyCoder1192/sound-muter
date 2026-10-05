plugins {
	id("net.neoforged.moddev")
	id("me.modmuss50.mod-publish-plugin")
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

publishMods {
	file = tasks.jar.flatMap { it.archiveFile }
	displayName = "${property("mod.name")} ${property("mod.version")} (NeoForge $mc)"
	version = project.version.toString()
	changelog = providers.gradleProperty("changelog").orElse(rootProject.file("CHANGELOG.md").readText())
	type = if (findProperty("publish.type") == "beta") BETA else STABLE
	modLoaders.add("neoforge")
	// missing a token -> only prints what it would upload
	dryRun = (!hasProperty("modrinth.token") && System.getenv("MODRINTH_TOKEN") == null) || System.getenv("GITHUB_TOKEN") == null

	modrinth {
		accessToken = providers.gradleProperty("modrinth.token").orElse(providers.environmentVariable("MODRINTH_TOKEN"))
		projectId = property("publish.modrinth_id") as String
		// only needed on the player's side, servers don't need it
		environment = CLIENT_ONLY
		minecraftVersions.addAll((property("publish.mc_targets") as String).split(" "))
	}

	// one release per version, same text as on Modrinth. ./release.sh fills GITHUB_TOKEN from `gh auth token`
	github {
		accessToken = providers.environmentVariable("GITHUB_TOKEN")
		repository = "SpeedyCoder1192/sound-muter"
		commitish = "main"
		tagName = project.version.toString()
	}
}
