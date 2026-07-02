package com.zinema.app.core.network.interceptors

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
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
 * Adds the `x-client-info` JSON blob plus the fixed session headers (blueprint §8.4),
 * shaped to match the real aoneroom client (verified against captured traffic):
 * x-play-mode/x-client-status = "1", `net` = NETWORK_*, and the extra client-info
 * fields (install_store, gaid, sp_code) + the nested X-* duplicates the server sees.
 *
 * The [API_CLIENT_*][Companion] identity values mirror the real MovieBox client so the
 * backend (and its guest token) accept our requests. Kids-profile flags come from
 * [SessionState] (OQ-04). No Accept header is sent — see [SigningInterceptor].
 */
class ClientInfoInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceIdProvider: DeviceIdProvider,
    private val sessionState: SessionState,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val kids = sessionState.isKidsProfile
        val familyMode = if (kids) "1" else "0"
        val request = chain.request().newBuilder()
            .header("x-client-info", buildClientInfoJson(kids))
            .header("x-child-uid", "")
            .header("x-play-mode", PLAY_MODE)
            .header("x-idle-data", "1")
            .header("x-family-mode", familyMode)
            .header("x-content-mode", familyMode)
            .header("x-client-status", CLIENT_STATUS)
            .header("User-Agent", buildUserAgent())
            .build()
        return chain.proceed(request)
    }

    private fun buildClientInfoJson(kids: Boolean): String {
        val familyMode = if (kids) "1" else "0"
        return buildJsonObject {
            put("package_name", API_CLIENT_PACKAGE)
            put("version_name", API_CLIENT_VERSION_NAME)
            put("version_code", API_CLIENT_VERSION_CODE)
            put("os", "android")
            put("os_version", Build.VERSION.RELEASE)
            put("install_ch", "google-play")
            put("device_id", deviceIdProvider.getHashedDeviceId())
            put("install_store", "gp")
            put("gaid", "")
            put("brand", Build.BRAND)
            put("model", Build.MODEL)
            put("system_language", Locale.getDefault().language)
            put("net", getNetworkType())
            put("region", Locale.getDefault().country)
            put("timezone", TimeZone.getDefault().id)
            put("sp_code", simOperatorCode())
            put("X-Child-UID", "")
            put("X-Play-Mode", PLAY_MODE)
            put("X-Idle-Data", "1")
            put("X-Family-Mode", familyMode)
            put("X-Content-Mode", familyMode)
        }.toString()
    }

    private fun buildUserAgent(): String =
        "$API_CLIENT_PACKAGE/$API_CLIENT_VERSION_CODE (Linux; U; Android ${Build.VERSION.RELEASE}; " +
            "${Locale.getDefault()}; ${Build.MODEL}; Build/${Build.ID})"

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "NETWORK_UNKNOWN"
        val network = cm.activeNetwork ?: return "NETWORK_NO"
        val caps = cm.getNetworkCapabilities(network) ?: return "NETWORK_UNKNOWN"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "NETWORK_WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "NETWORK_MOBILE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "NETWORK_ETHERNET"
            else -> "NETWORK_UNKNOWN"
        }
    }

    private fun simOperatorCode(): String = try {
        (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)?.simOperator.orEmpty()
    } catch (e: Exception) {
        ""
    }

    private companion object {
        // Backend-expected client identity — mirrors the real MovieBox client so the
        // aoneroom API + guest token accept us. Also avoids a min-version force-update.
        // Change these if you register your own app id with the backend.
        const val API_CLIENT_PACKAGE = "com.community.oneroom"
        const val API_CLIENT_VERSION_NAME = "3.0.15.0616.03"
        const val API_CLIENT_VERSION_CODE = 50020104
        const val PLAY_MODE = "1"
        const val CLIENT_STATUS = "1"
    }
}
