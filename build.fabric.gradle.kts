plugins {
	id("dev.kikugie.loom-back-compat")
	id("me.modmuss50.mod-publish-plugin")
}

val mc = stonecutter.current.version
val modId = property("mod.id") as String
val javaVersion = when {
	stonecutter.current.parsed >= "26.1" -> 25
	stonecutter.current.parsed >= "1.20.5" -> 21
	else -> 17
}

version = "${property("mod.version")}+$mc-fabric"
group = property("mod.group") as String
base.archivesName = modId

repositories {
	maven("https://maven.terraformersmc.com/releases/") { name = "Terraformers" }
}

sourceSets.main {
	java.exclude("**/platform/neoforge/**")
}

dependencies {
	minecraft("com.mojang:minecraft:$mc")
	loomx.applyMojangMappings()
	modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
	// optional, only for the config button in the mods list
	modCompileOnly("com.terraformersmc:modmenu:${property("deps.modmenu")}")
}

java {
	withSourcesJar()
	toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

tasks.processResources {
	exclude("META-INF/neoforge.mods.toml")
	val props = mapOf(
		"version" to project.version,
		"minecraft" to project.property("mod.mc_fabric"),
		"java" to javaVersion
	)
	inputs.properties(props)
	filesMatching(listOf("fabric.mod.json", "$modId.mixins.json")) { expand(props) }
}

tasks.jar {
	from(rootProject.file("LICENSE")) {
		rename { "${it}_$modId" }
	}
}

tasks.register<Copy>("collectJar") {
	group = "build"
	from(loomx.modJar.map { it.archiveFile })
	into(rootProject.layout.buildDirectory.dir("libs"))
}

publishMods {
	file = loomx.modJar.flatMap { it.archiveFile }
	displayName = "${property("mod.name")} ${property("mod.version")} (Fabric $mc)"
	version = project.version.toString()
	changelog = providers.gradleProperty("changelog").orElse(rootProject.file("CHANGELOG.md").readText())
	type = if (findProperty("publish.type") == "beta") BETA else STABLE
	modLoaders.add("fabric")
	// no token -> only prints what it would upload
	dryRun = !hasProperty("modrinth.token") && System.getenv("MODRINTH_TOKEN") == null

	modrinth {
		accessToken = providers.gradleProperty("modrinth.token").orElse(providers.environmentVariable("MODRINTH_TOKEN"))
		projectId = property("publish.modrinth_id") as String
		// only needed on the player's side, servers don't need it
		environment = CLIENT_ONLY
		minecraftVersions.addAll((property("publish.mc_targets") as String).split(" "))
		requires("fabric-api")
		optional("modmenu")
	}
}

// the client gametest API only exists on newer versions. On 1.21.11 the test harness never
// leaves the world loading screen (happens without this mod too), so it's skipped there
if (stonecutter.current.parsed >= "1.21.10" && mc != "1.21.11") {
	fabricApi {
		configureTests {
			createSourceSet = true
			modId = "$modId-test"
			enableGameTests = false
			enableClientGameTests = true
			eula = true
		}
	}
}
