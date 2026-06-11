package com.skynet.monitoring.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Payload pengiriman lokasi. Gunakan report_id (bukan task id).
 *
 * [recordedAt] = waktu fix GPS sebenarnya (ISO-8601 + offset zona, mis.
 * "2026-06-11T14:05:30+07:00"), bukan waktu kirim. Opsional & backward-compatible:
 * bila null, field tidak diserialisasi (Gson default) dan server memakai waktu terima.
 */
data class LocationRequest(
    @SerializedName("report_id") val reportId: Int,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("recorded_at") val recordedAt: String? = null,
)
