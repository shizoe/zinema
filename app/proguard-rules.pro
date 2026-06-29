# =====================================================================
# Zinema — release ProGuard / R8 rules (blueprint T-006, extended by T-063)
# =====================================================================

# --- Kotlinx Serialization -------------------------------------------
# Keep generated serializers and @Serializable metadata.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep every @Serializable model + its synthetic $$serializer.
-keep,includedescriptorclasses class com.zinema.app.**$$serializer { *; }
-keepclassmembers class com.zinema.app.** {
    *** Companion;
}
-keepclasseswithmembers @kotlinx.serialization.Serializable class com.zinema.app.** {
    <init>(...);
}

# --- Hilt / Dagger ----------------------------------------------------
# Hilt ships most of its own keep rules; these cover injection edges.
-keep,allowobfuscation,allowshrinking class javax.inject.** { *; }
-keep class dagger.hilt.internal.** { *; }
-dontwarn com.google.errorprone.annotations.**

# --- Retrofit / OkHttp ------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions

# --- Media3 (ExoPlayer) ----------------------------------------------
-dontwarn androidx.media3.**

# --- Strip all logging from release builds ---------------------------
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# --- Protect the request-signing key (blueprint T-006 / T-063) -------
# Obfuscate member + source names so the HMAC key constant and the signing
# routine are not trivially recoverable from a decompiled release APK.
# NOTE: string literals still live in the dex; T-063 may move the key to a
# native .so via JNI if the security review requires hardening beyond this.
-keep,allowobfuscation class com.zinema.app.core.network.interceptors.SigningInterceptor {
    <methods>;
}
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
