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
        // Proof-of-concept nodes: the current shipping build plus the highest-value backport target.
        // Fan the rest out (1.20.5, 1.21.3, 1.21.4, 1.21.5, ...) once these two are green.
        versions("1.21.1", "26.1.2")
        vcsVersion = "26.1.2"
    }
}

rootProject.name = "shulker-pocket"
