pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven {
            setUrl("https://artifacts.applovin.com/android")
            mavenContent {
                includeGroup("com.applovin.quality")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        mavenLocal {
            mavenContent {
                includeGroup("io.voodoo.apps")
            }
        }
        maven {
            setUrl("https://apps-sdk.voodoo-tech.io/android/")
            mavenContent {
                includeGroup("io.voodoo.apps")
            }
        }
        maven {
            setUrl("https://apps-sdk.voodoo-tech.io/android-dev/")
            mavenContent {
                includeGroup("io.voodoo.apps")
            }
        }
        maven {
            setUrl("https://android-sdk.is.com")
            content { includeGroup("com.ironsource.sdk") }
        }
        maven {
            setUrl("https://maven.ogury.co")
            content { includeGroupByRegex("co\\.ogury.*") }
        }
        maven {
            setUrl("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
            content { includeGroup("com.mbridge.msdk.oversea") }
        }
        maven {
            setUrl("https://artifactory.bidmachine.io/bidmachine")
            content {
                includeGroupByRegex("io\\.bidmachine.*")
                includeGroup("com.explorestack")
            }
        }
        maven {
            setUrl("https://artifact.bytedance.com/repository/pangle")
            content { includeGroup("com.pangle.global") }
        }
        maven {
            setUrl("https://cboost.jfrog.io/artifactory/chartboost-ads/")
            content { includeGroup("com.chartboost") }
        }
    }
}

rootProject.name = "Ads tools"
include(":sample")

include(":ads-api")
include(":ads-no-op")
include(":ads-applovin")
include(":ads-applovin-plugin-amazon")
include(":ads-compose")
include(":privacy")
