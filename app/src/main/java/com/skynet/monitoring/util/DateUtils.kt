package com.skynet.monitoring.util

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Format tanggal ISO-8601 backend (mis. "2025-06-01T08:00:00+07:00") → tampilan ringkas.
 * Memakai SimpleDateFormat (kompatibel minSdk 24, tanpa core library desugaring).
 */
object DateUtils {
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    private val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

    fun format(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return try {
            val date = inputFormat.parse(iso)
            if (date != null) outputFormat.format(date) else iso
        } catch (e: Exception) {
            iso
        }
    }
}
