pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
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
include(":network:core")
include(":network:retrofit")
