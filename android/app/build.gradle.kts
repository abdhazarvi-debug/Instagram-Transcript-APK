plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    buildFeatures { compose = true }
    namespace = "com.abdhazarvi.instatranscript"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.abdhazarvi.instatranscript"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.3.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }
    packaging {
        jniLibs {
            pickFirsts += "lib/arm64-v8a/libc++_shared.so"
            pickFirsts += "lib/x86_64/libc++_shared.so"
        }
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("dev.ffmpegkit-maintained:yt-dlp-android:2.0.2")
    implementation("dev.ffmpegkit-maintained:yt-dlp-android-compat:2.0.2")
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.7")
    // FFmpegKitConfig initializes SmartException on Android.
    implementation("com.arthenica:smart-exception-java:0.2.1")
    implementation("dev.ffmpegkit-maintained:whisper-android:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}