pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://storage.googleapis.com/tensorflow-nightly/github/maven")
        maven(url = "https://jitpack.io") // para MPAndroidChart
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://storage.googleapis.com/tensorflow-nightly/github/maven")
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "SmartAgroNutriApp"
include(":app")
