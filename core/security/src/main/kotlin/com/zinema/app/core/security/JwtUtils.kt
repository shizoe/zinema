package com.zinema.app.core.security

import org.json.JSONObject
import java.util.Base64

/**
 * Minimal, signature-agnostic JWT claim reader for client-side expiry awareness.
 * Does NOT verify the signature — the server is the source of truth on 401.
 */
object JwtUtils {

    /** `exp` claim in Unix seconds, or null if absent/unparseable. */
    fun expiresAtSeconds(token: String): Long? = claim(token) { it.optLong("exp").takeIf { v -> v > 0 } }

    /** `uid` claim, or null. Used to tell the baked-in guest token from a real one. */
    fun uid(token: String): Long? = claim(token) { it.optLong("uid").takeIf { v -> v > 0 } }

    fun isExpired(token: String): Boolean {
        val exp = expiresAtSeconds(token) ?: return true
        return nowSeconds() > exp
    }

    fun isExpiringSoon(token: String, withinDays: Int = 7): Boolean {
        val exp = expiresAtSeconds(token) ?: return true
        return exp < nowSeconds() + withinDays * SECONDS_PER_DAY
    }

    private inline fun <T> claim(token: String, extract: (JSONObject) -> T?): T? = try {
        val payload = token.split(".").getOrNull(1) ?: return null
        val padded = payload.padEnd(payload.length + (4 - payload.length % 4) % 4, '=')
        val json = String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
        extract(JSONObject(json))
    } catch (e: Exception) {
        null
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    private const val SECONDS_PER_DAY = 86_400L
}
