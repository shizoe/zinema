# Consumer ProGuard rules for core:network — applied to any module that
# depends on this library (ultimately :app) at release shrink time.

# Keep DTO models + generated serializers (kotlinx.serialization).
-keep,includedescriptorclasses class com.zinema.app.core.network.dto.**$$serializer { *; }
-keepclassmembers class com.zinema.app.core.network.dto.** {
    *** Companion;
    <init>(...);
}

# Protect the request-signing routine (see app/proguard-rules.pro, T-063).
-keep,allowobfuscation class com.zinema.app.core.network.interceptors.SigningInterceptor {
    <methods>;
}
