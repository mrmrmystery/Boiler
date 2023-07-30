plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.5.5"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "net.somewhatcity"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.thejocraft.net/releases/") {
        name = "tjcserver"
    }
    maven ("https://jitpack.io")
    maven ("https://maven.maxhenkel.de/repository/public")
    maven ("https://m2.dv8tion.net/releases")
}

dependencies {
    paperDevBundle("1.20-R0.1-SNAPSHOT")
    compileOnly("de.pianoman911:mapengine-api:1.5.2")
    compileOnly("de.pianoman911:mapengine-mediaext:1.0.5"){
        isTransitive = true
    }
    implementation("org.hibernate.orm:hibernate-core:6.2.5.Final")
    implementation("org.hibernate.orm:hibernate-community-dialects:6.2.5.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("dev.jorel:commandapi-bukkit-shade:9.0.3")
    implementation("io.javalin:javalin:5.6.0")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("de.maxhenkel.voicechat:voicechat-api:2.4.11")
    implementation("com.sedmelluq:lavaplayer:1.3.77")
    implementation("log4j:log4j:1.2.17")


}

tasks {

    shadowJar {
        relocate("dev.jorel.commandapi", "net.somewhatcity.mapdisplays.commandapi")
        dependencies {
            exclude(dependency("de.maxhenkel.voicechat:voicechat-api:2.4.11"))
        }

        doLast() {
            copy {
                from(shadowJar)
                into("./testserver/plugins")
            }
        }
    }

    assemble {
        dependsOn(reobfJar)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
