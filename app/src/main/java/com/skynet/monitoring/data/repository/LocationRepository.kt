package com.skynet.monitoring.data.repository

import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.LocationRequest
import com.skynet.monitoring.util.safeApiCall
import javax.inject.Inject

interface LocationRepository {
    /**
     * Kirim koordinat. Gunakan report_id (bukan task id).
     * [recordedAt] = waktu fix GPS (ISO-8601 + offset zona); null → server pakai waktu terima.
     */
    suspend fun sendLocation(
        reportId: Int,
        latitude: Double,
        longitude: Double,
        recordedAt: String? = null,
    ): Result<Unit>
}

class LocationRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) : LocationRepository {

    override suspend fun sendLocation(
        reportId: Int,
        latitude: Double,
        longitude: Double,
        recordedAt: String?,
    ): Result<Unit> =
        safeApiCall(gson) {
            api.sendLocation(LocationRequest(reportId, latitude, longitude, recordedAt))
        }.map { }
}
