plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

apply(from = "../gradlescripts/android-library.gradle")

val artifactGroupId by extra("io.voodoo.apps")
val artifactId by extra("ads-applovin")
val artifactVersion by extra(rootProject.extra.get("SDK_VER"))

android {
    namespace = "io.voodoo.apps.ads.applovin"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation(project(":ads-api"))
    api("com.applovin:applovin-sdk:12.5.0")
    api("com.github.appharbr:appharbr-android-sdk:2.19.3")
}

apply(from = "../gradlescripts/publisher.gradle")
