// core:domain — pure Kotlin/JVM. No Android imports allowed here.
// Holds domain models, repository interfaces, and use cases.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Flow / coroutines for repository interfaces (use the JVM-only artifact).
    implementation(libs.coroutines.core)
    // @Inject on use-case constructors (Hilt is wired in the Android modules).
    implementation(libs.javax.inject)
}
