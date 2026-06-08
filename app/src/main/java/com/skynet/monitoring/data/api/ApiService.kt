package com.skynet.monitoring.data.api

import com.skynet.monitoring.data.api.model.FcmTokenRequest
import com.skynet.monitoring.data.api.model.LocationRequest
import com.skynet.monitoring.data.api.model.LoginRequest
import com.skynet.monitoring.data.api.model.LoginResponse
import com.skynet.monitoring.data.api.model.MessageResponse
import com.skynet.monitoring.data.api.model.NotificationListResponse
import com.skynet.monitoring.data.api.model.TaskDetailResponse
import com.skynet.monitoring.data.api.model.TaskListResponse
import com.skynet.monitoring.data.api.model.UpdateStatusRequest
import com.skynet.monitoring.data.api.model.UpdateStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Semua endpoint backend. Memakai Response<T> agar repository bisa membaca HTTP status
 * (401/403/404/422) dan mem-parse error body. Header Authorization & Accept ditambahkan
 * otomatis oleh AuthInterceptor.
 */
interface ApiService {

    // ---- Auth ----
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<MessageResponse>

    @PUT("auth/fcm-token")
    suspend fun updateFcmToken(@Body body: FcmTokenRequest): Response<MessageResponse>

    // ---- Tugas ----
    @GET("tasks")
    suspend fun getTasks(): Response<TaskListResponse>

    @GET("tasks/{id}")
    suspend fun getTaskDetail(@Path("id") id: Int): Response<TaskDetailResponse>

    @POST("tasks/{id}/status")
    suspend fun updateTaskStatus(
        @Path("id") id: Int,
        @Body body: UpdateStatusRequest,
    ): Response<UpdateStatusResponse>

    // ---- GPS ----
    @POST("location")
    suspend fun sendLocation(@Body body: LocationRequest): Response<MessageResponse>

    // ---- Notifikasi ----
    @GET("notifications")
    suspend fun getNotifications(): Response<NotificationListResponse>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): Response<MessageResponse>
}
