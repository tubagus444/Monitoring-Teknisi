package com.skynet.monitoring.util

/**
 * Konstanta global aplikasi.
 */
object Constants {
    /** Interval pengiriman lokasi GPS saat status "sedang_memperbaiki" (milidetik). */
    const val LOCATION_INTERVAL_MS = 15_000L

    /** Interval tercepat penerimaan lokasi (milidetik). Batas bawah agar radio tidak boros. */
    const val LOCATION_FASTEST_INTERVAL_MS = 10_000L

    /**
     * Jarak minimum perpindahan (meter) sebelum lokasi baru dikirim. Saat teknisi diam di
     * satu titik, update ditahan — kunci hemat baterai selama memperbaiki.
     */
    const val LOCATION_MIN_DISPLACEMENT_M = 15f
}

/**
 * ID & nama channel notifikasi. Dipakai bersama oleh [com.skynet.monitoring.MonitoringApp],
 * LocationService (Fase 7), dan FirebaseService (Fase 9).
 */
object NotificationChannels {
    const val GPS_CHANNEL_ID = "gps_tracking"
    const val GPS_CHANNEL_NAME = "Pelacakan GPS"

    const val FCM_CHANNEL_ID = "fcm_default"
    const val FCM_CHANNEL_NAME = "Notifikasi Tugas"
}
