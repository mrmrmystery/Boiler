plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "Boiler"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}
include("api")
include("plugin")
include("platform-common")
include("platform-paper-1.20")
include("platform-paper-1.20.2")
include("platform-paper-1.20.3")
include("platform-paper-1.20.5")
