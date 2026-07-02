package com.zinema.app.core.network

import com.zinema.app.core.network.interceptors.SigningInterceptor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression test for the request-signing crypto (blueprint T-011).
 *
 * The expected HMAC was computed independently (HmacMD5 → Base64, standard
 * encoder) so this asserts against a fixed known-good value rather than
 * re-deriving it with the same code path.
 */
class SigningInterceptorTest {

    @Test
    fun signingKey_decodesTo30Bytes() {
        assertEquals(30, SigningInterceptor.KEY_BYTES.size)
    }

    @Test
    fun hmacMd5Base64_knownInput_matchesExpected() {
        val stringToSign = listOf(
            "GET",                                                              // method
            "*/*",                                                              // Accept
            "",                                                                 // Content-Type
            "",                                                                 // body length
            "1700000000000",                                                    // timestamp
            "",                                                                 // body md5
            "/wefeed-mobile-bff/tab-operating?page=1&tabId=13&version=v1",      // sorted path+query
        ).joinToString("\n")

        val signature = SigningInterceptor.hmacMd5Base64(SigningInterceptor.KEY_BYTES, stringToSign)

        assertEquals("t/O1mz5q2Vltpw5+7R8Cfg==", signature)
    }

    @Test
    fun md5Hex_knownInput_matchesExpected() {
        // Well-known MD5 of the ASCII string "hello".
        assertEquals("5d41402abc4b2a76b9719d911017c592", SigningInterceptor.md5Hex("hello"))
    }

    /**
     * Regression against a REAL captured, server-accepted request (POST search/v2):
     * proves our key + algorithm + field order + body-MD5 produce the exact signature
     * the server validated. Accept is empty, Content-Type carries charset=utf-8.
     */
    @Test
    fun signature_matchesCapturedServerAcceptedRequest() {
        val body = """{"page":1,"perPage":10,"keyword":"avatar"}""" // 42 bytes, as sent
        val stringToSign = listOf(
            "POST",
            "",                                     // Accept (none sent)
            "application/json; charset=utf-8",      // Content-Type
            body.toByteArray(Charsets.UTF_8).size.toString(), // 42
            "1782943006107",                        // ts (from captured signature)
            SigningInterceptor.md5Hex(body),
            "/wefeed-mobile-bff/subject-api/search/v2",
        ).joinToString("\n")

        val hmac = SigningInterceptor.hmacMd5Base64(SigningInterceptor.KEY_BYTES, stringToSign)

        assertEquals("LLrtZqVBXJ3GRFYpyrXcMA==", hmac)
    }
}
