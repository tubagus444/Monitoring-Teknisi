package com.skynet.monitoring.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Detik berlalu antara [startMillis] dan [nowMillis] (epoch ms), tak pernah negatif.
 * Fungsi murni — mudah diuji tanpa coroutine.
 */
fun elapsedSeconds(startMillis: Long, nowMillis: Long): Long =
    ((nowMillis - startMillis) / 1000).coerceAtLeast(0)

/**
 * Aliran durasi kerja: memancarkan jumlah detik berlalu sejak [startMillis], diperbarui tiap
 * [tickMillis]. Nilai selalu **dihitung dari selisih waktu** (`now - start`), bukan diakumulasi
 * per-tick — sehingga akurat walau layar/ViewModel dibuat ulang dan bebas drift bila tick tertunda.
 *
 * Emisi pertama terjadi langsung (tanpa menunggu satu tick) agar UI segera menampilkan durasi.
 *
 * @param startMillis waktu mulai (epoch ms). Bila null, dihitung dari [nowProvider] saat ini (mulai 0).
 * @param nowProvider sumber waktu kini; diinjeksi untuk pengujian (default jam sistem).
 * @param tickMillis jeda antar pembaruan.
 */
fun elapsedSecondsFlow(
    startMillis: Long?,
    nowProvider: () -> Long = System::currentTimeMillis,
    tickMillis: Long = 1000L,
): Flow<Long> = flow {
    val start = startMillis ?: nowProvider()
    while (true) {
        emit(elapsedSeconds(start, nowProvider()))
        delay(tickMillis)
    }
}
