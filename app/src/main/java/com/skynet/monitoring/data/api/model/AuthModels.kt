package com.skynet.monitoring.data.api.model

import com.google.gson.annotations.SerializedName

// ---- Request ----

data class LoginRequest(
    val email: String,
    val password: String,
)

data class FcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String,
)

// ---- Response ----

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: User,
)

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
)

/** Response umum yang hanya berisi field `message` (logout, fcm-token, location, dll). */
data class MessageResponse(
    @SerializedName("message") val message: String?,
)
