package com.skynet.monitoring.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Bentuk standar response error backend. `errors` hanya ada pada 422 (validasi).
 */
data class ErrorResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("errors") val errors: Map<String, List<String>>? = null,
)
