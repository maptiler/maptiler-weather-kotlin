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

    plugins {
        id("com.android.library") version "8.8.0"
        id("com.android.application") version "8.8.0"
        id("org.jetbrains.kotlin.android") version "2.1.21"
        id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
        id("org.jetbrains.dokka") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
        id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WeatherDemo"
include(":app")
include(":MapTilerWeather")
project(":MapTilerWeather").projectDir = file("../../MapTilerWeather")
