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
    implementation(project(":ads-api"))
    //noinspection UseTomlInstead
    api("com.applovin:applovin-sdk:${rootProject.extra.get("APPLOVIN")}")
    //noinspection UseTomlInstead
    api("com.github.appharbr:appharbr-android-sdk:${rootProject.extra.get("APPHRBR")}")
}

apply(from = "../gradlescripts/publisher.gradle")
