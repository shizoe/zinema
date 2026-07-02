package com.zinema.app.core.network.interceptors

import com.zinema.app.core.security.TokenStorage
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * Adds the `x-tr-signature` header (blueprint §8.2).
 *
 * Signature = "<ts>|<keyVersion>|<HmacMD5_Base64(stringToSign)>", where
 * stringToSign joins, by '\n':
 *   METHOD, Accept, Content-Type, bodyLen, ts, bodyMd5, sortedPathQuery
 *
 * `ts` is corrected for server clock skew via [TokenStorage.getServerTimeOffsetMs]
 * (OQ-01). Uses [java.util.Base64] (available at minSdk 26) rather than
 * android.util.Base64 so the crypto is unit-testable on the JVM (PHASE-1 §Deviations).
 */
class SigningInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val response = chain.proceed(signRequest(original))

        // Clock skew: the server rejects the signature with 407 (OQ-01). It reports
        // its own clock on every response, so correct the offset and retry once —
        // no extra round-trip to /app/config needed.
        if (response.code == HTTP_SIGN_REJECTED) {
            val serverMs = serverTimeMillis(response)
            if (serverMs != null) {
                tokenStorage.saveServerTimeOffsetMs(serverMs - System.currentTimeMillis())
                response.close()
                return chain.proceed(signRequest(original))
            }
        }
        return response
    }

    /** Reads the server clock from response headers (epoch ms), or null. */
    private fun serverTimeMillis(response: Response): Long? {
        response.header("req-arrive-time")?.trim()?.toLongOrNull()?.let { return it }
        return response.headers.getDate("date")?.time
    }

    /** Builds a copy of [original] carrying a fresh `x-tr-signature`. */
    private fun signRequest(original: Request): Request {
        // Read the body without consuming it (it can only be read once).
        val bodyStr = original.body?.let { body ->
            val buffer = okio.Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        } ?: ""

        val ts = System.currentTimeMillis() + tokenStorage.getServerTimeOffsetMs()
        val sortedPathQuery = buildSortedPathQuery(original.url)
        // Verified against captured traffic: the real client signs the actual Accept
        // header, which is empty (it sends none) — NOT "*/*".
        val accept = original.header("Accept") ?: ""
        // For Retrofit @Body POSTs the Content-Type lives on the body, not as a
        // request header — fall back to it so POST signatures match what is sent.
        val contentType = original.header("Content-Type")
            ?: original.body?.contentType()?.toString()
            ?: ""
        val bodyLen = if (bodyStr.isNotEmpty()) {
            bodyStr.toByteArray(Charsets.UTF_8).size.toString()
        } else {
            ""
        }
        val bodyMd5 = if (bodyStr.isNotEmpty()) md5Hex(bodyStr.take(MAX_BODY_HASH_CHARS)) else ""

        val stringToSign = listOf(
            original.method.uppercase(),
            accept,
            contentType,
            bodyLen,
            ts.toString(),
            bodyMd5,
            sortedPathQuery,
        ).joinToString("\n")

        val hmacB64 = hmacMd5Base64(KEY_BYTES, stringToSign)
        val signature = "$ts|$KEY_VERSION|$hmacB64"

        // Reconstruct the body if present (writeTo consumed our copy, not the
        // original, but rebuilding keeps the request immutable + explicit).
        val newBody = if (bodyStr.isNotEmpty()) {
            bodyStr.toRequestBody(original.body!!.contentType())
        } else {
            original.body
        }

        return original.newBuilder()
            .method(original.method, newBody)
            .header("x-tr-signature", signature)
            .build()
    }

    private fun buildSortedPathQuery(url: HttpUrl): String {
        val path = url.encodedPath
        if (url.querySize == 0) return path
        val sorted = url.queryParameterNames
            .sorted()
            .joinToString("&") { name ->
                val decodedName = URLDecoder.decode(name, "UTF-8")
                val value = url.queryParameter(name)?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
                "$decodedName=$value"
            }
        return "$path?$sorted"
    }

    companion object {
        private const val KEY_B64 = "76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O"
        private const val KEY_VERSION = 2
        private const val MAX_BODY_HASH_CHARS = 102400
        private const val HTTP_SIGN_REJECTED = 407

        internal val KEY_BYTES: ByteArray = Base64.getDecoder().decode(KEY_B64)

        internal fun hmacMd5Base64(key: ByteArray, data: String): String {
            val mac = Mac.getInstance("HmacMD5")
            mac.init(SecretKeySpec(key, "HmacMD5"))
            val digest = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(digest)
        }

        internal fun md5Hex(input: String): String {
            val digest = MessageDigest.getInstance("MD5")
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
