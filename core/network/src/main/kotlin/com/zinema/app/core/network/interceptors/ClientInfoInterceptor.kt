package com.zinema.app.core.network.interceptors

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.zinema.app.core.network.BuildConfig
import com.zinema.app.core.domain.session.SessionState
import com.zinema.app.core.security.DeviceIdProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Adds the `x-client-info` JSON blob plus the fixed session headers (blueprint §8.4).
 *
 * Kids-profile flags come from [SessionState] (OQ-04). The `x-client-info` value
 * is built as an explicit JsonObject — the blueprint's `mapOf<String, Any>()`
 * has no kotlinx serializer, so it is constructed via buildJsonObject instead
 * (PHASE-1 §Deviations). [context] is needed to resolve the active network type.
 */
class ClientInfoInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceIdProvider: DeviceIdProvider,
    private val sessionState: SessionState,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("x-client-info", buildClientInfoJson())
            .header("x-child-uid", "")
            .header("x-play-mode", "2")
            .header("x-idle-data", "1")
            .header("x-family-mode", if (sessionState.isKidsProfile) "1" else "0")
            .header("x-content-mode", if (sessionState.isKidsProfile) "1" else "0")
            .header("x-client-status", "0")
            .header("User-Agent", buildUserAgent())
            .header("Accept", "*/*")
            .build()
        return chain.proceed(request)
    }

    private fun buildClientInfoJson(): String = buildJsonObject {
        put("package_name", "com.zinema.app")
        put("version_name", BuildConfig.VERSION_NAME)
        put("version_code", BuildConfig.VERSION_CODE)
        put("os", "android")
        put("os_version", Build.VERSION.RELEASE)
        put("install_ch", "google-play")
        put("device_id", deviceIdProvider.getHashedDeviceId())
        put("brand", Build.BRAND)
        put("model", Build.MODEL)
        put("system_language", Locale.getDefault().language)
        put("net", getNetworkType())
        put("region", Locale.getDefault().country)
        put("timezone", TimeZone.getDefault().id)
    }.toString()

    private fun buildUserAgent(): String =
        "com.zinema.app/${BuildConfig.VERSION_CODE} (Linux; U; Android ${Build.VERSION.RELEASE}; " +
            "${Locale.getDefault()}; ${Build.MODEL}; Build/${Build.ID})"

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "unknown"
        val network = cm.activeNetwork ?: return "none"
        val caps = cm.getNetworkCapabilities(network) ?: return "unknown"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
    }
}
