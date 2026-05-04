import java.net.Inet4Address
import java.net.NetworkInterface

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.free2party"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.free2party"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = "0.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val frequency = System.getenv("updateFrequency") ?: "5000"
        buildConfigField("long", "updateFrequency", "${frequency}L")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true

            val virtualDeviceEmulator = false
            val computerIp = if (virtualDeviceEmulator) "10.0.2.2" else getLocalIpAddress()
            buildConfigField("String", "COMPUTER_IP", "\"$computerIp\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // --- Core & Platform ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // --- Jetpack Compose (UI) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // --- Architecture & Navigation ---
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // --- Firebase & Backend ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.common)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)
    implementation(libs.play.integrity)

    // --- Google Authentication & Credentials Manager ---
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // --- Image Loading ---
    implementation(libs.coil.compose)

    // --- Desugaring ---
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // --- Unit Testing ---
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)

    // --- Instrumentation Testing ---
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.mockk.agent)
    androidTestImplementation(libs.mockk.android)

    // --- Debug Tools ---
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

fun getLocalIpAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        val addresses = mutableListOf<String>()
        
        for (networkInterface in interfaces) {
            if (networkInterface.isLoopback || !networkInterface.isUp) continue

            val interfaceAddresses = networkInterface.inetAddresses
            for (address in interfaceAddresses) {
                if (address is Inet4Address && !address.isLoopbackAddress) {
                    val hostAddress = address.hostAddress
                    // Prioritize common local network ranges
                    if (hostAddress.startsWith("192.168.") || hostAddress.startsWith("10.")) {
                        return hostAddress
                    }
                    addresses.add(hostAddress)
                }
            }
        }
        return addresses.firstOrNull() ?: "10.0.2.2"
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "10.0.2.2"
}
