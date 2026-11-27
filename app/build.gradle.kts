import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releasePropsFile = rootProject.file("release.properties")
val releaseProps = Properties().apply {
    if (releasePropsFile.exists()) {
        releasePropsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.musicplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vzt.sonora"
        minSdk = 26
        targetSdk = 35
        versionCode = 43
        versionName = "1.2"
    }

    signingConfigs {
        create("shared") {
            val storeFileName = releaseProps.getProperty("storeFile")
            if (!storeFileName.isNullOrBlank()) {
                storeFile = rootProject.file(storeFileName)
                storePassword = releaseProps.getProperty("storePassword")
                keyAlias = releaseProps.getProperty("keyAlias")
                keyPassword = releaseProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            val sharedSigning = signingConfigs.findByName("shared")
            if (sharedSigning?.storeFile?.exists() == true) {
                signingConfig = sharedSigning
            }
        }
        release {
            isMinifyEnabled = false
            val sharedSigning = signingConfigs.findByName("shared")
            if (sharedSigning?.storeFile?.exists() == true) {
                signingConfig = sharedSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")

    val lifecycleVersion = "2.8.6"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-jvm:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")

    val media3Version = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
}

configurations.all {
    resolutionStrategy {
        force(
            "androidx.lifecycle:lifecycle-runtime-ktx:2.8.6",
            "androidx.lifecycle:lifecycle-runtime-android:2.8.6",
            "androidx.lifecycle:lifecycle-common:2.8.6",
            "androidx.lifecycle:lifecycle-common-jvm:2.8.6",
            "androidx.lifecycle:lifecycle-viewmodel:2.8.6",
            "androidx.lifecycle:lifecycle-viewmodel-android:2.8.6",
            "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6",
            "androidx.lifecycle:lifecycle-livedata-core:2.8.6"
        )
    }
}
