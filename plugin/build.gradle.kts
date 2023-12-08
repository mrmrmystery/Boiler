plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

repositories {
    mavenCentral()
    maven("https://repo.thejocraft.net/releases/") {
        name = "tjcserver"
    }
    maven ("https://maven.maxhenkel.de/repository/public")
    maven ("https://m2.dv8tion.net/releases")
    maven ("https://jitpack.io")
    maven ("https://repo.plo.su")
}

dependencies {
    implementation("org.projectlombok:lombok:1.18.28")
    runtimeOnly(project(":platform-paper-1.20"))
    runtimeOnly(project(":platform-paper-1.20.2"))
    runtimeOnly(project(":platform-paper-1.20.3"))

    implementation("dev.jorel:commandapi-bukkit-shade:9.2.0")
    implementation("de.maxhenkel.voicechat:voicechat-api:2.4.11")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    api(project(":platform-common"))

    compileOnly("de.pianoman911:mapengine-api:1.6.2")
    compileOnly("de.pianoman911:mapengine-mediaext:1.1.0")
}

tasks {
    shadowJar {
        destinationDirectory.set(rootProject.buildDir.resolve("libs"))
        archiveBaseName.set(rootProject.name)

        relocate("dev.jorel.commandapi", "net.somewhatcity.boiler.commandapi")
        dependencies {
            exclude(dependency("de.maxhenkel.voicechat:voicechat-api:2.4.11"))
        }
    }

    assemble {
        dependsOn(shadowJar)
    }

}

bukkit {
    main = "$group.boiler.core.BoilerPlugin"
    apiVersion = "1.20"
    authors = listOf("mrmrmystery")
    name = rootProject.name
    depend = listOf("MapEngine", "MapMediaExt", "voicechat")
    version = "2.0.2"
    //softDepend = listOf("voicechat")
}