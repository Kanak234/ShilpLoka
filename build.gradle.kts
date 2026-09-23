plugins {
    id("java")
    id("application")
    kotlin("jvm") version "1.9.20"
}

group = "shilploka"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("shilploka.core.Game")
    applicationDefaultJvmArgs = listOf("-Xmx2g")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // LWJGL 3 core
    val lwjglVersion = "3.4.2"
    val lwjglNatives = "natives-linux"

    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-windows")

    // JOML - Java OpenGL Math Library (latest: 1.10.9)
    implementation("org.joml:joml:1.10.9")

    // SLF4J + Logback
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

distributions {
    main {
        contents {
            from("LICENSE", "README.md", "docs/ASSET_LICENSES.md")
        }
    }
}

tasks.register("packageAll") {
    dependsOn("build", "distZip", "distTar")
    doLast {
        val distDir = file("dist")
        distDir.mkdirs()
        fileTree("build/distributions").forEach { archive ->
            archive.copyTo(File(distDir, archive.name), overwrite = true)
        }
        println("All distribution packages successfully generated in dist/:")
        distDir.listFiles()?.forEach { println(" - " + it.name + " (" + (it.length() / 1024) + " KB)") }
    }
}