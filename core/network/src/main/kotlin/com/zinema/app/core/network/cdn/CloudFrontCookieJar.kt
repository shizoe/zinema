package com.zinema.app.core.network.cdn

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Injects the CloudFront `play_auth` cookies for signed stream requests, scoped
 * strictly to the streaming CDN hosts (blueprint §8.5).
 *
 * Only loads cookies for the two stream hosts; never persists cookies received
 * from responses. Setting cookies for any other host is rejected.
 */
class CloudFrontCookieJar : CookieJar {

    private val cdnHosts = setOf("sacdn.hakunaymatata.com", "msacdn.hakunaymatata.com")
    private val cookies = mutableMapOf<String, List<Cookie>>()

    fun setPlayAuthCookies(host: String, policy: String, signature: String, keyPairId: String) {
        require(host in cdnHosts) { "Attempted to set CDN cookies for unauthorized host: $host" }
        cookies[host] = listOf(
            buildCookie(host, "CloudFront-Policy", policy),
            buildCookie(host, "CloudFront-Signature", signature),
            buildCookie(host, "CloudFront-Key-Pair-Id", keyPairId),
        )
    }

    fun clearCookies() = cookies.clear()

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        if (url.host in cdnHosts) cookies[url.host] ?: emptyList() else emptyList()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) { /* no-op */ }

    private fun buildCookie(host: String, name: String, value: String): Cookie =
        Cookie.Builder().domain(host).name(name).value(value).build()
}
