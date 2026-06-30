package com.zinema.app.core.network.cdn

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Hostname allowlist enforcement (blueprint §8.6).
 *
 * Every image/stream URL is checked against the known CDN hosts; private/loopback
 * IP ranges are rejected outright to block SSRF-style redirects. Uses OkHttp 4's
 * `String.toHttpUrlOrNull()` rather than the removed `HttpUrl.parse` (PHASE-1
 * §Deviations).
 */
object CdnValidator {

    private val ALLOWED_API_HOSTS = setOf("api6.aoneroom.com")

    private val ALLOWED_CDN_HOSTS = setOf(
        "pbcdn.aoneroom.com",
        "pacdn.aoneroom.com",
        "macdn.aoneroom.com",
        "sacdn.hakunaymatata.com",
        "msacdn.hakunaymatata.com",
        "cacdn.hakunaymatata.com",
    )

    // RFC-1918 + loopback prefixes — reject any of these.
    private val PRIVATE_PREFIXES = listOf("10.", "192.168.", "172.16.", "172.17.", "127.", "0.")

    /** True if [url] is an allowed API or CDN host and not a private/loopback address. */
    fun isAllowed(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val host = url.toHttpUrlOrNull()?.host ?: return false
            if (PRIVATE_PREFIXES.any { host.startsWith(it) }) return false
            host in ALLOWED_CDN_HOSTS || host in ALLOWED_API_HOSTS
        } catch (e: Exception) {
            false
        }
    }

    /** True only for the signed video-stream CDN hosts (used to gate playback). */
    fun isStreamHost(url: String): Boolean {
        val host = url.toHttpUrlOrNull()?.host ?: return false
        return host == "sacdn.hakunaymatata.com" || host == "msacdn.hakunaymatata.com"
    }
}
