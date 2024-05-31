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
            url = uri("https://artifacts.applovin.com/android")
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
        mavenLocal {
            mavenContent {
                includeGroup("io.voodoo.apps")
            }
        }
        maven {
            url = uri("https://apps-sdk.voodoo-tech.io/android/")
            mavenContent {
                includeGroup("io.voodoo.apps")
            }
        }
        maven {
            url = uri("https://apps-sdk.voodoo-tech.io/android-dev/")
            mavenContent {
                includeGroup("io.voodoo.apps")
            }
        }
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "Ads tools"
include(":app")
include(":ads-api")
include(":ads-applovin")
include(":ads-applovin-plugin-amazon")
include(":ads-applovin-compose")
