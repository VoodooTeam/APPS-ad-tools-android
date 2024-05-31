plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

apply(from = "../gradlescripts/android-library.gradle")

val artifactGroupId by extra("io.voodoo.apps")
val artifactId by extra("ads-applovin-compose")
val artifactVersion by extra(rootProject.extra.get("SDK_VER"))

android {
    namespace = "io.voodoo.apps.ads.applovin.compose"

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
}

dependencies {
    compileOnly(project(":ads-api"))
    compileOnly(project(":ads-applovin"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.foundation)
}

apply(from = "../gradlescripts/publisher.gradle")
