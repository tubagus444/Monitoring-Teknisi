package com.skynet.monitoring.data.api.model

/**
 * Kategori tugas sebagaimana dikembalikan backend (field `category`).
 * Menentukan apakah tugas terkait seorang pelanggan (kartu kontak + galeri foto rumah)
 * atau gangguan jaringan/pemeliharaan (tanpa data pelanggan).
 *
 * [label] adalah teks Indonesia untuk ditampilkan sebagai badge kategori.
 */
enum class TaskCategory(val apiValue: String, val label: String) {
    PELANGGAN("pelanggan", "Gangguan Pelanggan"),
    JARINGAN("jaringan", "Gangguan Jaringan/Infrastruktur"),
    PEMELIHARAAN("pemeliharaan", "Pemeliharaan");

    companion object {
        /** Parse nilai kategori backend; `null` bila kosong/tidak dikenal. */
        fun from(value: String?): TaskCategory? = entries.firstOrNull { it.apiValue == value }
    }
}
