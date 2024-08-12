plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.jetbrainsKotlinSerialization)
    //id("applovin-quality-service")
}

//applovin {
//    apiKey = "«ad-review-key»"
//}

android {
    namespace = "io.voodoo.apps.ads"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.voodoo.apps.ads"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Androidx
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Misc
    implementation(libs.kotlin.serialization)
    implementation(libs.kotlin.collections)
    implementation(libs.coil)
    implementation(libs.bundles.retrofit)

    // Ads
    val sdkVersion = rootProject.ext.get("SDK_VER").toString()
    if (true) {
        implementation(project(":ads-api"))
        implementation(project(":ads-applovin"))
        implementation(project(":ads-admob"))
        implementation(project(":ads-compose"))
        implementation(project(":ads-applovin-plugin-amazon"))
    } else {
        val sdkAmazonPluginVersion = rootProject.ext.get("SDK_AMAZON_PLUGIN_VER").toString()
        implementation("io.voodoo.apps", "ads-api", sdkVersion)
        implementation("io.voodoo.apps", "ads-applovin", sdkVersion)
        implementation("io.voodoo.apps", "ads-compose", sdkVersion)
        implementation("io.voodoo.apps", "ads-applovin-plugin-amazon", sdkAmazonPluginVersion)
    }

    // Ad networks
    // Supported by apphrbr
    implementation("com.applovin.mediation:google-adapter:23.2.0.0")
    implementation("com.applovin.mediation:bidmachine-adapter:2.6.0.1")
    implementation("com.applovin.mediation:chartboost-adapter:9.7.0.0")
    implementation("com.applovin.mediation:facebook-adapter:6.17.0.0")
    implementation("com.applovin.mediation:fyber-adapter:8.2.7.0")
    implementation("com.applovin.mediation:google-ad-manager-adapter:23.2.0.0")
    implementation("com.applovin.mediation:inmobi-adapter:10.7.4.0")
    implementation("com.applovin.mediation:mintegral-adapter:16.6.71.0")
    implementation("com.applovin.mediation:bytedance-adapter:5.9.0.2.0")
    implementation("com.applovin.mediation:vungle-adapter:7.3.2.0")
    implementation("com.bigossp:bigo-ads:4.7.0")
    implementation("com.bigossp:max-mediation:4.7.0.0")

    // Not supported
    implementation("com.applovin.mediation:ironsource-adapter:7.9.0.0.0")
    implementation("com.applovin.mediation:line-adapter:2024.5.15.0")
    implementation("com.applovin.mediation:ogury-presage-adapter:5.7.0.0")
    implementation("com.applovin.mediation:unityads-adapter:4.12.0.0")
    implementation("com.mobilefuse.sdk:mobilefuse-adapter-applovin:1.7.4.0")

    // Sub-dependencies
    implementation("com.google.android.gms:play-services-base:16.1.0") // for chartboost
    implementation("com.squareup.picasso:picasso:2.71828") // for inmobi
    implementation("androidx.recyclerview:recyclerview:1.1.0") // for inmobi

    // Consent
    if (true) {
        implementation(project(":privacy"))
    } else {
        implementation("io.voodoo.apps", "privacy", sdkVersion)
    }

    // Test
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        freeCompilerArgs += "-opt-in=kotlin.time.ExperimentalTime"
        freeCompilerArgs += "-opt-in=kotlin.contracts.ExperimentalContracts"
        freeCompilerArgs += "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"

        // Compose opt-in
        freeCompilerArgs += "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        freeCompilerArgs += "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        freeCompilerArgs += "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
        freeCompilerArgs += "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        freeCompilerArgs += "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
    }
}
