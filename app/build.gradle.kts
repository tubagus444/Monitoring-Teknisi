import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// URL backend untuk HP fisik dibaca dari local.properties (tidak ikut commit), key `deviceBaseUrl`.
// Default mengarah ke backend produksi di VPS (https://skynet-monitoring.tech/api/).
// Bisa di-override di local.properties bila perlu uji coba lokal.
val deviceBaseUrl: String = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}.getProperty("deviceBaseUrl") ?: "https://skynet-monitoring.tech/api/"

android {
    namespace = "com.skynet.monitoring"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.skynet.monitoring"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Dua varian environment. Pilih di Android Studio: "Build Variants" → emulatorDebug / deviceDebug / deviceRelease.
    // applicationId TIDAK diubah per-flavor agar google-services.json (Firebase) tetap cocok.
    flavorDimensions += "env"
    productFlavors {
        create("emulator") {
            dimension = "env"
            // 10.0.2.2 = host machine dari emulator Android.
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/api/\"")
        }
        create("device") {
            dimension = "env"
            // Default backend VPS resmi, bisa di-override via local.properties (key: deviceBaseUrl).
            buildConfigField("String", "BASE_URL", "\"$deviceBaseUrl\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Gunakan debug signingConfig agar APK release bisa langsung di-install & dijalankan di HP fisik via Android Studio.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Retrofit + OkHttp + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // DataStore Preferences (token + user)
    implementation(libs.androidx.datastore.preferences)

    // Room Database (Offline Cache)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Firebase Cloud Messaging
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // GPS — FusedLocationProviderClient
    implementation(libs.play.services.location)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Coil (image loading)
    implementation(libs.coil.compose)

    // Unit test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
