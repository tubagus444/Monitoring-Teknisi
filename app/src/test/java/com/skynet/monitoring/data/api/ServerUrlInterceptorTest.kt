package com.skynet.monitoring.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NormalizeServerUrlTest {

    @Test
    fun `kosong atau blank menghasilkan null`() {
        assertNull(normalizeServerUrl(null))
        assertNull(normalizeServerUrl(""))
        assertNull(normalizeServerUrl("   "))
    }

    @Test
    fun `host port tanpa skema dianggap http`() {
        val url = normalizeServerUrl("192.168.0.105:8000")!!
        assertEquals("http", url.scheme)
        assertEquals("192.168.0.105", url.host)
        assertEquals(8000, url.port)
    }

    @Test
    fun `skema https dipertahankan dengan port default 443`() {
        val url = normalizeServerUrl("https://api.skynet.id")!!
        assertEquals("https", url.scheme)
        assertEquals("api.skynet.id", url.host)
        assertEquals(443, url.port)
    }

    @Test
    fun `spasi di tepi dipangkas`() {
        val url = normalizeServerUrl("  http://10.0.2.2:8000  ")!!
        assertEquals("10.0.2.2", url.host)
        assertEquals(8000, url.port)
    }
}
