@file:Suppress("UnstableApiUsage")

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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Spotify App Remote SDK is distributed as a local .aar (see app/libs and README).
        flatDir { dirs("app/libs") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SpotZones"
include(":app")
