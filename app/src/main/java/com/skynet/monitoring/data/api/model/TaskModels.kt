package com.skynet.monitoring.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Satu tugas. Dipakai untuk list maupun detail — [workLogs] & [housePhotos] hanya terisi di
 * endpoint detail.
 * `status` di endpoint ini hanya "ditugaskan" atau "sedang_memperbaiki".
 * `address` adalah teks biasa, BUKAN koordinat.
 *
 * Sejak Modul Pelanggan backend, tugas punya [category] ("pelanggan" | "jaringan" |
 * "pemeliharaan"). Untuk judul tampilan PAKAI [headline] (selalu terisi) — bukan [customer],
 * yang `null` pada tugas non-pelanggan. Field pelanggan ([customer], [phone], [ipAddress],
 * [subscriptionPackage]) semuanya bisa `null` pada tugas jaringan/pemeliharaan.
 */
data class Task(
    @SerializedName("id") val id: Int,
    @SerializedName("report_id") val reportId: Int,
    @SerializedName("status") val status: String,
    @SerializedName("customer") val customer: String?,
    @SerializedName("address") val address: String,
    @SerializedName("damage_type") val damageType: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("assigned_at") val assignedAt: String?,
    @SerializedName("work_logs") val workLogs: List<WorkLog>? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("headline") val headline: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("ip_address") val ipAddress: String? = null,
    @SerializedName("subscription_package") val subscriptionPackage: String? = null,
    @SerializedName("house_photos") val housePhotos: List<String> = emptyList(),
    @SerializedName("repair_photos") val repairPhotos: List<RepairPhoto> = emptyList(),
) {
    /** Judul tampilan aman-null: [headline] bila ada, jatuh ke [customer], lalu placeholder. */
    val displayTitle: String
        get() = headline ?: customer ?: "Tugas #$reportId"
}

data class WorkLog(
    @SerializedName("status") val status: String,
    @SerializedName("technician") val technician: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("logged_at") val loggedAt: String?,
)

data class TaskListResponse(
    @SerializedName("data") val data: List<Task>,
)

data class TaskDetailResponse(
    @SerializedName("data") val data: Task,
)

/**
 * Body update status. [status] diisi dari [StatusAction.apiValue] (lihat status mapping di
 * CLAUDE.md). [description] = catatan pekerjaan opsional (maks 1000 char); `null` → tidak dikirim
 * (Gson meng-omit field null secara default).
 */
data class UpdateStatusRequest(
    @SerializedName("status") val status: String,
    @SerializedName("description") val description: String? = null,
)

/**
 * Foto bukti hasil kerja (semua kategori). Berbeda dari [Task.housePhotos] yang hanya berisi URL
 * string — di sini tiap foto adalah objek dengan caption & teknisi pengunggah.
 * Muncul di endpoint DETAIL pada field `repair_photos` (urut naik waktu unggah).
 */
data class RepairPhoto(
    @SerializedName("url") val url: String,
    @SerializedName("caption") val caption: String? = null,
    @SerializedName("technician") val technician: String? = null,
    @SerializedName("uploaded_at") val uploadedAt: String? = null,
)

/** Respons 201 dari unggah foto (bukti pekerjaan & rumah pelanggan). */
data class PhotoUploadResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: RepairPhoto?,
)

data class UpdateStatusResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?,
)
