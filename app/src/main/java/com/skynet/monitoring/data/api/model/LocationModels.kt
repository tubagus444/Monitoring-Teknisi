package com.skynet.monitoring.data.api.model

import com.google.gson.annotations.SerializedName

/** Payload pengiriman lokasi. Gunakan report_id (bukan task id). */
data class LocationRequest(
    @SerializedName("report_id") val reportId: Int,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
)
