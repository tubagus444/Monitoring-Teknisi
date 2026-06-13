package com.skynet.monitoring.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * Satu tugas. Dipakai untuk list maupun detail — [workLogs] hanya terisi di endpoint detail.
 * `status` di endpoint ini hanya "ditugaskan" atau "sedang_memperbaiki".
 * `address` adalah teks biasa, BUKAN koordinat.
 */
data class Task(
    @SerializedName("id") val id: Int,
    @SerializedName("report_id") val reportId: Int,
    @SerializedName("status") val status: String,
    @SerializedName("customer") val customer: String,
    @SerializedName("address") val address: String,
    @SerializedName("damage_type") val damageType: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("assigned_at") val assignedAt: String?,
    @SerializedName("work_logs") val workLogs: List<WorkLog>? = null,
)

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

/** Body update status. Diisi dari [StatusAction.apiValue] (lihat status mapping di CLAUDE.md). */
data class UpdateStatusRequest(
    @SerializedName("status") val status: String,
)

data class UpdateStatusResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?,
)
