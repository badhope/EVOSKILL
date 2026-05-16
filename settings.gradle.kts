pluginManagement {
    repositories {
        maven { url = uri("http://127.0.0.1:8888") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven { url = uri("http://127.0.0.1:8888") }
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
