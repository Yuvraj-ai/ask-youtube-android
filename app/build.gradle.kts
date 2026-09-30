import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.askyoutube.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.askyoutube.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        // Credentials come from ~/.android/askyoutube-test.properties, which is
        // outside the repository. Without that file the release build silently
        // falls back to the debug key so a fresh clone still builds something
        // installable rather than failing on a missing secret.
        create("release") {
            val props = Properties()
            val f = File(System.getProperty("user.home"), ".android/askyoutube-test.properties")
            if (f.exists()) f.inputStream().use { props.load(it) }
            val store = props.getProperty("storeFile")
            if (store != null && File(store).exists()) {
                storeFile = File(store)
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 on. The app uses no reflection and no dynamic class loading, so
            // shrinking is safe, and it takes a large bite out of Compose.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.forEach { cfg ->
                if (cfg.name == "release" && cfg.storeFile != null) {
                    signingConfig = cfg
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    // Real org.json on the JVM. The android.jar stub only throws "not mocked",
    // so unit tests that touch the transcript parser need the actual library.
    testImplementation(libs.json)
}
