plugins {
    id("com.gradleup.shadow") version "8.3.11"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("com.modrinth.minotaur") version "2.+"
}

repositories {
    mavenCentral()
    maven("https://repo.thejocraft.net/releases/") {
        name = "tjcserver"
    }
    maven ("https://maven.maxhenkel.de/repository/public")
    maven ("https://m2.dv8tion.net/releases")
    maven ("https://jitpack.io")
    //maven("https://repo.plasmoverse.com/releases")
    //maven("https://repo.plasmoverse.com/snapshots")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.15.2")
    testImplementation("io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT")
    testImplementation("de.pianoman911:mapengine-api:1.8.7")
    testImplementation("org.bytedeco:javacv:1.5.10") { isTransitive = false }
    testImplementation("org.bytedeco:ffmpeg:6.1.1-1.5.10")
    val nativeOs = when {
        System.getProperty("os.name").lowercase().contains("win") -> "windows"
        System.getProperty("os.name").lowercase().contains("mac") -> "macosx"
        else -> "linux"
    }
    val nativeArch = if (System.getProperty("os.arch") in listOf("aarch64", "arm64")) "arm64" else "x86_64"
    testRuntimeOnly("org.bytedeco:javacpp:1.5.10:$nativeOs-$nativeArch")
    testRuntimeOnly("org.bytedeco:ffmpeg:6.1.1-1.5.10:$nativeOs-$nativeArch")
    /*
    runtimeOnly(project(":platform-paper-1.20"))
    runtimeOnly(project(":platform-paper-1.20.2"))
    runtimeOnly(project(":platform-paper-1.20.3"))
    runtimeOnly(project(":platform-paper-1.20.5"))
    runtimeOnly(project(":platform-paper-1.21.2"))
     */

    implementation("de.maxhenkel.voicechat:voicechat-api:2.4.11")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("uk.co.caprica:vlcj:4.8.2")
    implementation("org.freedesktop.gstreamer:gst1-java-core:1.4.0")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation("io.javalin:javalin:6.4.0")

    implementation("io.netty:netty-buffer:4.1.116.Final")
    implementation("io.netty:netty-codec:4.1.116.Final")

    //compileOnlyApi("org.bytedeco:javacv-platform:1.5.11")

    api(project(":api"))

    compileOnly("de.pianoman911:mapengine-api:1.8.7")
    implementation("com.google.code.gson:gson:2.12.1")

    compileOnly("de.pianoman911:mapengine-mediaext:1.1.3")
    //compileOnly("su.plo.voice.api:server:2.1.0-SNAPSHOT")

}

tasks {
    test {
        useJUnitPlatform()
        systemProperty("java.awt.headless", "true")
    }
    shadowJar {
        destinationDirectory.set(rootProject.buildDir.resolve("libs"))
        archiveBaseName.set(rootProject.name)
        isZip64 = true

        relocate("okhttp3", "net.somewhatcity.boiler.okhttp")
        relocate("kotlin", "net.somewhatcity.boiler.kotlin")
        dependencies {
            exclude(dependency("de.maxhenkel.voicechat:voicechat-api:2.4.11"))
            //exclude(dependency("com.formdev:flatlaf:3.3"))
        }
    }

    assemble {
        dependsOn(shadowJar)
    }
}

tasks.register<JavaExec>("performanceBenchmark") {
    group = "verification"
    description = "Compare the original allocating raster adapter with the reusable adapter (no Minecraft server)."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("net.somewhatcity.boiler.core.RasterBenchmark")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    jvmArgs("-Xms256m", "-Xmx512m", "-Djava.awt.headless=true")
    args(rootProject.layout.buildDirectory.file("reports/performance/raster.csv").get().asFile.absolutePath)
}

bukkit {
    main = "$group.boiler.core.BoilerPlugin"
    apiVersion = "1.21"
    authors = listOf("mrmrmystery")
    name = rootProject.name
    depend = listOf("MapEngine", "MapMediaExt")
    softDepend = listOf("PlasmoVoice", "voicechat")
    version = rootProject.version.toString()
    //softDepend = listOf("voicechat")
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("9R8jBgPj")
    versionNumber.set(rootProject.version.toString())
    versionType.set("release")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(listOf("1.21.6"))
    loaders.addAll(listOf("paper", "purpur"))
    dependencies {
        required.project("fCDPz9mZ")
        required.project("FMg8aS6R")
        optional.project("9eGKb6K1")
        optional.project("1bZhdhsH")
    }
}
