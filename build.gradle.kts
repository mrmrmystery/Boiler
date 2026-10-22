plugins {
    id("java-library")
    id("maven-publish")

}

tasks["jar"].enabled = false

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "net.somewhatcity"
    version = "3.0.3"

    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.thejocraft.net/releases/") {
            name = "tjcserver"
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
    }

    java {
        withSourcesJar()
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))

        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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
        repositories.maven("https://repo.somewhatcity.net/releases") {
            name = "somewhatcity"
            authentication { create<BasicAuthentication>("basic") }
            credentials(PasswordCredentials::class)
        }
    }
}


