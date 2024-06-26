plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

apply(from = "../gradlescripts/android-library.gradle")

val artifactGroupId by extra("io.voodoo.apps")
val artifactId by extra("ads-applovin-plugin-amazon")
val artifactVersion by extra(rootProject.extra.get("SDK_AMAZON_PLUGIN_VER"))

android {
    namespace = "io.voodoo.apps.ads.applovin.plugin.amazon"
}

dependencies {
    val amazonAdapterVersion = rootProject.extra.get("AMAZON_ADAPTER")
    val amazonVersion = amazonAdapterVersion.toString().substringBeforeLast('.')

    compileOnly(project(":ads-api"))
    compileOnly(project(":ads-applovin"))

    //noinspection UseTomlInstead
    api("com.applovin.mediation:amazon-tam-adapter:$amazonAdapterVersion")
    //noinspection UseTomlInstead
    api("com.amazon.android:aps-sdk:$amazonVersion")
}

apply(from = "../gradlescripts/publisher.gradle")
