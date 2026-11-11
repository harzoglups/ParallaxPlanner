import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin module: no Android plugin, no framework dependency (see docs/04-architecture.md).
plugins {
    kotlin("jvm") version "2.0.21"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
    // Test only: parses spec/test-vectors.json.
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

tasks.test {
    // The vectors file is platform-neutral and lives at the repository root (NF-05).
    val vectors = rootProject.file("../spec/test-vectors.json")
    inputs.file(vectors)
    systemProperty("testVectors", vectors.absolutePath)
    testLogging {
        events("passed", "skipped", "failed")
    }
}
