plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.dokka)
    `maven-publish`
}

android {
    namespace = "com.pedro.rtspserver"
    compileSdk = 37

    defaultConfig {
        minSdk = 16
        lint.targetSdk = 37
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing {
        singleVariant("release")
    }
}

kotlin {
    jvmToolchain(17)
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("release") {
                // Applies the component for the release build variant.
                from(components["release"])

                // You can then customize attributes of the publication as shown below.
                groupId = project.group.toString()
                artifactId = "rtspserver"
                version = project.version.toString()
            }
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.rootEncoder.library)
    implementation(libs.ktor.network)
    implementation(libs.ktor.network.tls)
}

// Fork development only (-Pspc.localRootEncoder=true): swap the pinned JitPack
// RootEncoder for the sibling checkout's `publishToMavenLocal` output. Release
// builds never use it — JitPack builds from the tag with the pin above.
if (providers.gradleProperty("spc.localRootEncoder").orNull == "true") {
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            listOf(
                "library", "encoder", "common", "rtsp", "rtmp", "srt", "udp",
                "extra-sources", "whip",
            ).forEach { m ->
                substitute(module("com.github.speculor-app.RootEncoder:$m"))
                    .using(module("com.github.pedroSG94:$m:2.8.0"))
                    .because("local fork development build")
            }
        }
    }
}
