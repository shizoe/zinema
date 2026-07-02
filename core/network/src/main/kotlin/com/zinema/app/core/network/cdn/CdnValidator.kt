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

    // First-party CDN domains. Images/posters live on *.aoneroom.com (pbcdn/pacdn/
    // macdn…); signed video streams + external subtitles live on *.hakunaymatata.com
    // and are served from a rotating pool of edge nodes (sacdn/msacdn/hcdn1/hcdn2/
    // hcdn3/bcdn/cacdn…), so we allow the whole domain rather than a brittle host list.
    private val ALLOWED_CDN_SUFFIXES = listOf(".aoneroom.com", ".hakunaymatata.com")

    private const val STREAM_CDN_SUFFIX = ".hakunaymatata.com"

    // RFC-1918 + loopback prefixes — reject any of these.
    private val PRIVATE_PREFIXES = listOf("10.", "192.168.", "172.16.", "172.17.", "127.", "0.")

    /** True if [url] is an allowed API or CDN host and not a private/loopback address. */
    fun isAllowed(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val host = url.toHttpUrlOrNull()?.host ?: return false
            if (PRIVATE_PREFIXES.any { host.startsWith(it) }) return false
            host in ALLOWED_API_HOSTS || ALLOWED_CDN_SUFFIXES.any { host.endsWith(it) }
        } catch (e: Exception) {
            false
        }
    }

    /** True only for the signed video-stream CDN domain (used to gate playback). */
    fun isStreamHost(url: String): Boolean {
        val host = url.toHttpUrlOrNull()?.host ?: return false
        if (PRIVATE_PREFIXES.any { host.startsWith(it) }) return false
        return host.endsWith(STREAM_CDN_SUFFIX)
    }
}
