plugins {
    id("java-library")
    id("maven-publish")

    id("io.papermc.paperweight.userdev") version "1.5.5" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

tasks["jar"].enabled = false

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "net.somewhatcity"
    version = "1.0-SNAPSHOT"

    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.thejocraft.net/releases/") {
            name = "tjcserver"
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()

        copy {
            from("./build/libs/Boiler-1.0-SNAPSHOT-all.jar")
            into("./testserver/plugins")
        }
    }

    java {
        withSourcesJar()
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }
}

subprojects {
    publishing {
        publications.create<MavenPublication>("maven${project.name}") {
            artifactId = "${rootProject.name}-${project.name}".lowercase()
            from(components["java"])
        }
        repositories {
            mavenLocal()
        }
    }
}
