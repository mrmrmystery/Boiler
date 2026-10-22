rootProject.name = "Boiler"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}
include("api")
include("plugin")
