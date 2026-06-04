plugins {
    // Picks the remapping loom for obfuscated MC (<26.1) and the no-remap loom for 26.1+,
    // and exposes a uniform modImplementation / applyMojangMappings / modJar API for both.
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin") version "1.1.0"
}

// Per Stonecutter: do NOT set group here.
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "shulker-pocket"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1"   -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else                          -> JavaVersion.VERSION_17
}

// Modrinth/CurseForge game versions this jar covers (used later for publishing).
val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.terraformersmc.com/") { name = "Terraformers" }
}

loom {
    // Separate `main` (common + server) and `client` source sets.
    splitEnvironmentSourceSets()

    mods {
        create("shulker_pocket") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

val fabricApiVersion: String = sc.properties["deps.fabric_api"]
val modmenuVersion: String = sc.properties["deps.modmenu"]

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // No-op on unobfuscated 26.1; applies Mojang mappings on the remapped older versions.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("com.terraformersmc:modmenu:$modmenuVersion")

    testImplementation("net.fabricmc:fabric-loader-junit:${property("deps.fabric_loader")}")
}

tasks.test {
    useJUnitPlatform()
}

val mcCompat: String = sc.properties["mod.mc_compat"]
val javaMajor = requiredJava.majorVersion

// fabric.mod.json lives in the main source set, the client mixins.json in the client source set, so
// configure both processResources and processClientResources. Each filesMatching is a no-op on the
// task that lacks that file.
tasks.withType<org.gradle.language.jvm.tasks.ProcessResources>().configureEach {
    inputs.property("version", version)
    inputs.property("minecraft", mcCompat)
    inputs.property("java", javaMajor)

    filesMatching("fabric.mod.json") {
        expand(mapOf(
            "version" to version,
            "minecraft" to mcCompat,
            "java" to javaMajor,
        ))
    }
    filesMatching("*.mixins.json") {
        expand(mapOf("java" to "JAVA_$javaMajor"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = requiredJava.majorVersion.toInt()
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

// Modrinth publishing. Each node uploads one version (file `0.3.0+<mc>`) with its own gameVersions.
// Fan out across all nodes with the chiseledPublishMods task (see stonecutter.gradle.kts). Without a
// MODRINTH_TOKEN (local runs) it dry-runs: config is validated but nothing is uploaded.
publishMods {
    file = loomx.modJar.flatMap { it.archiveFile }
    version = project.version.toString()
    type = me.modmuss50.mpp.ReleaseType.STABLE
    displayName = "Shulker Pocket ${project.version}"
    modLoaders.add("fabric")
    changelog = "Multi-version release built from one source tree. See CHANGELOG.md in the repository."
    dryRun = !providers.environmentVariable("MODRINTH_TOKEN").isPresent

    modrinth {
        projectId = property("mod.modrinth_id") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(compatibleVersions)
        requires("fabric-api")
        optional("modmenu")
    }
}
