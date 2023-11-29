pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Tyche"
include(":app")
include(":account")
include(":pool")
include(":bet")
include(":core")
include(":ui")
include(":session")
include(":data:pool")
include(":data:bet")
