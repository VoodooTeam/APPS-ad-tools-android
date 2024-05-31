plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

apply(from = "../gradlescripts/android-library.gradle")

val artifactGroupId by extra("io.voodoo.apps")
val artifactId by extra("ads")
val artifactVersion by extra(rootProject.extra.get("SDK_VER"))

android {
    namespace = "io.voodoo.apps.ads"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Ads
    implementation("com.applovin:applovin-sdk:12.4.2")
    implementation("com.github.appharbr:appharbr-android-sdk:2.19.0")

    // Amazon requires specific implementation code...
    // TODO: extract in a separate gradle module
    implementation("com.applovin.mediation:amazon-tam-adapter:9.9.3.2")
    implementation("com.amazon.android:aps-sdk:9.9.3")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

apply(from = "../gradlescripts/publisher.gradle")
