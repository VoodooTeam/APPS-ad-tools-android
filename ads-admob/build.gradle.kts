plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

apply(from = "../gradlescripts/android-library.gradle")

val artifactGroupId by extra("io.voodoo.apps")
val artifactId by extra("ads-admob")
val artifactVersion by extra(rootProject.extra.get("SDK_VER"))

android {
    namespace = "io.voodoo.apps.ads.admob"
}

dependencies {
    implementation(project(":ads-api"))
    implementation("com.google.android.gms:play-services-ads:23.2.0")

}

apply(from = "../gradlescripts/publisher.gradle")
