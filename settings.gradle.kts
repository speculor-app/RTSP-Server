pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Fork development only (-Pspc.localRootEncoder=true): resolve the
        // RootEncoder fork from a `publishToMavenLocal` of the sibling checkout
        // instead of the pinned JitPack tag, so a change spanning both forks
        // can be verified on a phone before either immutable tag is cut.
        if (providers.gradleProperty("spc.localRootEncoder").orNull == "true") {
            mavenLocal()
        }
    }
}

rootProject.name = "RTSP-Server"
include(":app", ":rtspserver")
