plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

apply(from = "../gradlescripts/android-library.gradle")

val artifactGroupId by extra("io.voodoo.apps")
val artifactId by extra("ads-applovin-plugin-amazon")
val artifactVersion by extra(rootProject.extra.get("SDK_VER"))

android {
    namespace = "io.voodoo.apps.ads.applovin.plugin.amazon"
}

dependencies {
    compileOnly(project(":ads-api"))
    compileOnly("com.applovin:applovin-sdk:12.5.0")
    implementation("com.applovin.mediation:amazon-tam-adapter:9.9.3.2")
    implementation("com.amazon.android:aps-sdk:9.9.3")
}

apply(from = "../gradlescripts/publisher.gradle")
