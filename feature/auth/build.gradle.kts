// feature:auth — login screen, profile selector, guest mode.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.zinema.app.feature.auth"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")

        // Guest-mode JWT for AuthViewModel.loginAsGuest() (blueprint T-037).
        // Sourced from the Gradle property ZINEMA_GUEST_JWT; empty by default.
        val guestJwt = (project.findProperty("ZINEMA_GUEST_JWT") as String?) ?: ""
        buildConfigField("String", "GUEST_JWT", "\"$guestJwt\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.tv)
    implementation(libs.compose.viewmodel)
    implementation(libs.compose.lifecycle.runtime)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.coil.compose)
    implementation(libs.coroutines.android)
}
