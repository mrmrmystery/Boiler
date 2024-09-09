import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    id("io.papermc.paperweight.userdev")
}

dependencies {
    api(project(":platform-common"))
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
}

paperweight {
    reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.reobfJar {
    outputJar.set(tasks.jar.map { it.outputs.files.singleFile }.get())
    enabled = false
}