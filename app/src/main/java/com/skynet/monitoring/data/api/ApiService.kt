package com.skynet.monitoring.data.api

import com.skynet.monitoring.data.api.model.FcmTokenRequest
import com.skynet.monitoring.data.api.model.LocationRequest
import com.skynet.monitoring.data.api.model.LoginRequest
import com.skynet.monitoring.data.api.model.LoginResponse
import com.skynet.monitoring.data.api.model.MessageResponse
import com.skynet.monitoring.data.api.model.NotificationListResponse
import com.skynet.monitoring.data.api.model.PhotoUploadResponse
import com.skynet.monitoring.data.api.model.TaskDetailResponse
import com.skynet.monitoring.data.api.model.TaskListResponse
import com.skynet.monitoring.data.api.model.UpdateStatusRequest
import com.skynet.monitoring.data.api.model.UpdateStatusResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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

    /**
     * Unggah foto bukti hasil kerja (semua kategori). Multipart — nama part file wajib `photo`,
     * caption opsional. Sukses → 201.
     */
    @Multipart
    @POST("tasks/{id}/photos")
    suspend fun uploadRepairPhoto(
        @Path("id") id: Int,
        @Part photo: MultipartBody.Part,
        @Part("caption") caption: RequestBody?,
    ): Response<PhotoUploadResponse>

    /**
     * Unggah foto rumah pelanggan (hanya tugas kategori `pelanggan`; backend balas 422 bila bukan).
     * Multipart — nama part file wajib `photo`. Hasil muncul di `house_photos` pada detail.
     */
    @Multipart
    @POST("tasks/{id}/house-photos")
    suspend fun uploadHousePhoto(
        @Path("id") id: Int,
        @Part photo: MultipartBody.Part,
        @Part("caption") caption: RequestBody?,
    ): Response<PhotoUploadResponse>

    // ---- GPS ----
    @POST("location")
    suspend fun sendLocation(@Body body: LocationRequest): Response<MessageResponse>

    // ---- Notifikasi ----
    @GET("notifications")
    suspend fun getNotifications(): Response<NotificationListResponse>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): Response<MessageResponse>
}
