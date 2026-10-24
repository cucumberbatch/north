plugins {
    java
    application
}

description = "North Engine Core"

// LWJGL configuration
val lwjglVersion = "3.3.3"
val lwjglNatives = providers.gradleProperty("lwjglNatives").orNull ?: run {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()

    when {
        osName.contains("win") ->
            if (osArch.contains("aarch64")) "natives-windows-arm64" else "natives-windows"

        osName.contains("mac") ->
            if (osArch.contains("aarch64")) "natives-macos-arm64" else "natives-macos"

        osName.contains("linux") -> when {
            osArch.contains("aarch64") -> "natives-linux-arm64"
            osArch.contains("arm") -> "natives-linux-arm32"
            else -> "natives-linux"
        }

        else -> error(
            "Unsupported OS for LWJGL natives: $osName ($osArch). " +
                "Pass -PlwjglNatives=<classifier> to override."
        )
    }
}

dependencies {
    // LWJGL core modules
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")

    // LWJGL natives for the current platform
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")

    // Math library
    implementation("org.joml:joml:1.9.9")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.4.7")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("org.north.core.architecture.MainThread")
}

// Process resources with filtering
tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("**/north.properties") {
        expand(
            "project.name" to project.name,
            "project.version" to project.version
        )
    }
}
