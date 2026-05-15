pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Banana-Toolbox"

include(":app")
include(":core-common")
include(":core-ui")
include(":feature-filemanager")
include(":feature-appmanager")
include(":feature-network")
include(":feature-tools")
