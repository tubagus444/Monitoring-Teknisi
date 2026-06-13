package com.skynet.monitoring.util

import java.text.SimpleDateFormat
import java.util.Date
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

    /**
     * Waktu epoch (millis UTC) → ISO-8601 dengan offset zona device, mis.
     * "2026-06-11T14:05:30+07:00". Dipakai LocationService untuk `recorded_at` (waktu fix GPS).
     * SimpleDateFormat dibuat per panggilan agar aman dipakai lintas thread (IO).
     */
    fun toIso8601(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date(epochMillis))

    /**
     * ISO-8601 backend → epoch millis UTC, atau null bila kosong/format tak dikenal.
     * Dipakai layar Working untuk menghitung durasi dari waktu mulai perbaikan (`work_log`),
     * agar timer tetap akurat walau layar dibuka ulang.
     */
    fun toEpochMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            inputFormat.parse(iso)?.time
        } catch (e: Exception) {
            null
        }
    }
}
