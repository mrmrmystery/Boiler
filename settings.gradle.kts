rootProject.name = "Boiler"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}
include("api")
include("plugin")
include("platform-paper-1.20")
include("platform-command")
include("platform-common")
include("platform-paper-1.20.2")
