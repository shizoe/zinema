package com.zinema.app.core.network

import android.app.Application
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.ErrorResult
import com.zinema.app.core.domain.session.SessionState
import com.zinema.app.core.network.cdn.CdnValidator
import com.zinema.app.core.network.cdn.CloudFrontCookieJar
import com.zinema.app.core.network.interceptors.AuthInterceptor
import com.zinema.app.core.network.interceptors.ClientInfoInterceptor
import com.zinema.app.core.network.interceptors.LogScrubInterceptor
import com.zinema.app.core.network.interceptors.SigningInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

/**
 * Hilt bindings for the network stack (blueprint §8.7).
 *
 * Application-interceptor order (blueprint T-016): ClientInfo → Auth → Signing →
 * LogScrub. Signing must run last so it sees the final header set. The shared
 * [OkHttpClient] is also handed to Coil so image loads reuse the same pipeline.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCloudFrontCookieJar(): CloudFrontCookieJar = CloudFrontCookieJar()

    // SessionState lives in the processor-free core:domain module, so its
    // singleton binding is declared here (SingletonComponent) rather than via an
    // @Inject constructor. Feature modules inject the same instance.
    @Provides
    @Singleton
    fun provideSessionState(): SessionState = SessionState()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        clientInfoInterceptor: ClientInfoInterceptor,
        authInterceptor: AuthInterceptor,
        signingInterceptor: SigningInterceptor,
        logScrubInterceptor: LogScrubInterceptor,
        cookieJar: CloudFrontCookieJar,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(clientInfoInterceptor)   // 1. add headers first
        .addInterceptor(authInterceptor)         // 2. then auth
        .addInterceptor(signingInterceptor)      // 3. signing last (needs final headers)
        .addInterceptor(logScrubInterceptor)     // 4. scrub hook
        .addNetworkInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
                // Never emit sensitive header values to logs (T-019).
                LogScrubInterceptor.SENSITIVE_HEADERS.forEach { redactHeader(it) }
            },
        )
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        return Retrofit.Builder()
            .baseUrl("https://api6.aoneroom.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideCoil(
        context: Application,
        okHttpClient: OkHttpClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(okHttpClient)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(200L * 1024 * 1024) // 200MB
                .build()
        }
        .components {
            // Enforce the CDN allowlist on every image load (blueprint §8.7).
            add(
                object : coil.intercept.Interceptor {
                    override suspend fun intercept(
                        chain: coil.intercept.Interceptor.Chain,
                    ): coil.request.ImageResult {
                        val url = chain.request.data as? String
                        if (url != null && !CdnValidator.isAllowed(url)) {
                            return ErrorResult(
                                null,
                                chain.request,
                                SecurityException("CDN not in allowlist: $url"),
                            )
                        }
                        return chain.proceed(chain.request)
                    }
                },
            )
        }
        .build()
}
