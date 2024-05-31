plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

apply(from = "../gradlescripts/android-library.gradle")

val artifactGroupId by extra("io.voodoo.apps")
val artifactId by extra("ads-api")
val artifactVersion by extra(rootProject.extra.get("SDK_VER"))

android {
    namespace = "io.voodoo.apps.ads.api"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    compileOnly("androidx.annotation:annotation:1.8.0")
}

apply(from = "../gradlescripts/publisher.gradle")
