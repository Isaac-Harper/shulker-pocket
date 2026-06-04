pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.5"
    id("dev.kikugie.loom-back-compat") version "0.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // One build node per binary-compatibility breakpoint from 1.20.5 to current. Each node's
        // jar covers a patch range (see mod.mc_releases in stonecutter.properties.toml). 1.21.11 is
        // the ResourceLocation -> Identifier rename boundary; 26.1 is the unobfuscation boundary.
        versions(
            "1.20.6",   // covers 1.20.5, 1.20.6
            "1.21.1",   // covers 1.21, 1.21.1
            "1.21.3",   // covers 1.21.2, 1.21.3
            "1.21.4",
            "1.21.5",
            "1.21.8",   // covers 1.21.6, 1.21.7, 1.21.8
            "1.21.10",  // covers 1.21.9, 1.21.10
            "1.21.11",
            "26.1.2",   // covers 26.1, 26.1.1, 26.1.2
        )
        vcsVersion = "26.1.2"
    }
}

rootProject.name = "shulker-pocket"
