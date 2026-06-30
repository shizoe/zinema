// core:network — OkHttp, Retrofit, signing interceptor, API service, DTOs.
// Depends on core:security for the JWT token + server-time offset.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.zinema.app.core.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")

        // ClientInfoInterceptor reads the app version from BuildConfig. Library
        // BuildConfig has no VERSION_* by default, so we expose them here. Keep in
        // sync with :app versionName/versionCode (see PHASE-1 doc §Deviations).
        buildConfigField("String", "VERSION_NAME", "\"1.0.0\"")
        buildConfigField("int", "VERSION_CODE", "1")
    }

    // ClientInfoInterceptor / NetworkModule read BuildConfig (version + DEBUG flag).
    buildFeatures {
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
    implementation(project(":core:security"))
    // SessionState (kids-profile flags) lives in core:domain so feature modules
    // can mutate it too (see PHASE-1 doc §Deviations).
    implementation(project(":core:domain"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Shared OkHttp client is also handed to Coil's ImageLoader (NetworkModule §8.7).
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
}
