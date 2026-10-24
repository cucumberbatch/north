plugins {
    base
}

allprojects {
    group = "org.alter.north"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(11)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
