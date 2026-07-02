package com.zinema.app.core.network

import com.zinema.app.core.network.cdn.CdnValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Final-checklist coverage: allowed hosts pass; IP literals + private ranges fail. */
class CdnValidatorTest {

    @Test
    fun allowedCdnAndApiHosts_pass() {
        assertTrue(CdnValidator.isAllowed("https://pbcdn.aoneroom.com/poster.jpg"))
        assertTrue(CdnValidator.isAllowed("https://macdn.aoneroom.com/img.png"))
        assertTrue(CdnValidator.isAllowed("https://api6.aoneroom.com/wefeed-mobile-bff/app/config"))
        assertTrue(CdnValidator.isAllowed("https://sacdn.hakunaymatata.com/stream.mpd"))
    }

    @Test
    fun unknownHost_fails() {
        assertFalse(CdnValidator.isAllowed("https://evil.example.com/x.jpg"))
    }

    @Test
    fun ipLiteralAndPrivateRanges_fail() {
        assertFalse(CdnValidator.isAllowed("https://1.2.3.4/x.jpg"))
        assertFalse(CdnValidator.isAllowed("http://192.168.1.10/x.jpg"))
        assertFalse(CdnValidator.isAllowed("http://10.0.0.5/x.jpg"))
        assertFalse(CdnValidator.isAllowed("http://172.16.0.1/x.jpg"))
        assertFalse(CdnValidator.isAllowed("http://127.0.0.1/x.jpg"))
    }

    @Test
    fun blankUrl_fails() {
        assertFalse(CdnValidator.isAllowed(""))
    }

    @Test
    fun isStreamHost_onlyStreamCdns() {
        assertTrue(CdnValidator.isStreamHost("https://sacdn.hakunaymatata.com/v.mpd"))
        assertTrue(CdnValidator.isStreamHost("https://msacdn.hakunaymatata.com/v.mp4"))
        assertFalse(CdnValidator.isStreamHost("https://pbcdn.aoneroom.com/poster.jpg"))
        assertFalse(CdnValidator.isStreamHost("https://api6.aoneroom.com/x"))
    }
}
