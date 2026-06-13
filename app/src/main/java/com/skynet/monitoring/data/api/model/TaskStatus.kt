package com.skynet.monitoring.data.api.model

/**
 * Status tugas sebagaimana **disimpan & dikembalikan backend** (field `status` di response).
 * Transisi searah: [ASSIGNED] → [IN_PROGRESS] → [DONE].
 *
 * Memetakan nilai mentah API ([apiValue]) ke konstanta type-safe agar tidak ada string
 * `"ditugaskan"`/`"sedang_memperbaiki"`/`"selesai"` yang tersebar & rawan typo di UI.
 */
enum class TaskStatus(val apiValue: String) {
    ASSIGNED("ditugaskan"),
    IN_PROGRESS("sedang_memperbaiki"),
    DONE("selesai");

    companion object {
        /** Parse nilai status backend; `null` bila tidak dikenal (mis. status baru dari server). */
        fun from(value: String?): TaskStatus? = entries.firstOrNull { it.apiValue == value }
    }
}

/**
 * Aksi perubahan status yang **dikirim Android** ke `POST /api/tasks/{id}/status`.
 *
 * Berbeda dari [TaskStatus]: nilai yang dikirim ([apiValue]) di-mapping backend ke status
 * tersimpan (`in_progress` → `sedang_memperbaiki`, `done` → `selesai`). Lihat CLAUDE.md.
 */
enum class StatusAction(val apiValue: String) {
    START("in_progress"),
    FINISH("done"),
}
