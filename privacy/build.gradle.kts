plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

apply(from = "../gradlescripts/android-library.gradle")

val artifactGroupId by extra("io.voodoo.apps")
val artifactId by extra("privacy")
val artifactVersion by extra(rootProject.extra.get("SDK_VER"))

android {
    namespace = "io.voodoo.apps.ads.privacy"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.common.java8)

    //noinspection UseTomlInstead
    implementation("com.sourcepoint.cmplibrary:cmplibrary:7.8.4")
    //noinspection UseTomlInstead
    implementation("com.google.code.gson:gson:2.11.0")
}

apply(from = "../gradlescripts/publisher.gradle")
