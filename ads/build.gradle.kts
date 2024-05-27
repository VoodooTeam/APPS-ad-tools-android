plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "io.voodoo.apps.ads"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    // Androidx
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Ads
    //noinspection UseTomlInstead
    implementation("com.applovin:applovin-sdk:12.4.2")
    //noinspection UseTomlInstead
    implementation("com.github.appharbr:appharbr-android-sdk:2.19.0")
    // Amazon requires specific implementation code...
    //noinspection UseTomlInstead
    implementation("com.applovin.mediation:amazon-tam-adapter:9.9.3.2")
    //noinspection UseTomlInstead
    implementation("com.amazon.android:aps-sdk:9.9.3")

    implementation("com.jakewharton.timber:timber:5.0.1")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
