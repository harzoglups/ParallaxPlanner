import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Written by hand at each release; the release tag must be "v" + this value (Q23).
val appVersionName = "0.1.2"

// 1.2.3 gives 10203: always increasing as long as minor and patch stay below 100.
fun versionCodeOf(versionName: String): Int {
    val (major, minor, patch) = versionName.split(".").map { it.toInt() }
    return major * 10_000 + minor * 100 + patch
}

// Release signing (Q23): environment variables in the release workflow,
// or android/keystore.properties (ignored by Git) on the maintainer's machine.
// Without either, the release build stays unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(name: String): String? =
    providers.environmentVariable(name).orNull ?: keystoreProperties.getProperty(name)

android {
    namespace = "com.cussou.parallax"
    compileSdk = 36

    defaultConfig {
        // Permanent once published: do not change.
        applicationId = "com.cussou.parallax"
        minSdk = 26 // NF-10
        targetSdk = 36
        versionCode = versionCodeOf(appVersionName)
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val storeFile = signingValue("RELEASE_KEYSTORE_FILE")
        if (storeFile != null) {
            create("release") {
                this.storeFile = rootProject.file(storeFile)
                storePassword = signingValue("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // BuildConfig.VERSION_NAME is shown in the About section.
        buildConfig = true
    }

    androidResources {
        // Languages the app is translated into (F-13).
        localeFilters += listOf("en", "fr")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    // Per-app language on every supported Android version (F-10).
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    // Match files: storage and export share one JSON format (F-65).
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Compose UI tests pull an older Espresso that crashes on Android 16+.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}
