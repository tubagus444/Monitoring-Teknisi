package com.skynet.monitoring.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `null atau blank mengembalikan strip`() {
        assertEquals("-", DateUtils.format(null))
        assertEquals("-", DateUtils.format(""))
        assertEquals("-", DateUtils.format("   "))
    }

    @Test
    fun `string tak valid dikembalikan apa adanya`() {
        assertEquals("bukan-tanggal", DateUtils.format("bukan-tanggal"))
    }

    @Test
    fun `iso valid diformat ulang ke pola tampilan`() {
        // Tidak meng-assert jam/tanggal spesifik (bergantung zona JVM); cukup pastikan
        // string sudah diformat (bukan dikembalikan apa adanya) & cocok pola "dd MMM yyyy, HH:mm".
        val input = "2025-06-01T08:00:00+07:00"
        val out = DateUtils.format(input)
        assertNotEquals(input, out)
        assertTrue("hasil tak terduga: $out", Regex("""\d{2} \S{3} \d{4}, \d{2}:\d{2}""").matches(out))
    }

    @Test
    fun `toIso8601 menghasilkan format ISO-8601 dengan offset zona`() {
        val out = DateUtils.toIso8601(0L)
        assertTrue(
            "hasil tak terduga: $out",
            Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}""").matches(out),
        )
    }
}
