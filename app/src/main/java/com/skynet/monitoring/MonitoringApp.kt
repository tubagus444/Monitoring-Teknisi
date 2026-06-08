package com.skynet.monitoring

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.skynet.monitoring.util.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class utama. Dianotasi [HiltAndroidApp] sebagai root dependency graph Hilt.
 * Membuat notification channel saat startup (wajib Android 8+ / API 26).
 */
@HiltAndroidApp
class MonitoringApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)

        // Channel untuk foreground service GPS — importance rendah (tidak berbunyi/getar).
        val gpsChannel = NotificationChannel(
            NotificationChannels.GPS_CHANNEL_ID,
            NotificationChannels.GPS_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Notifikasi saat GPS aktif selama perbaikan"
            setShowBadge(false)
        }

        // Channel untuk notifikasi tugas (FCM) — importance tinggi agar muncul heads-up.
        val fcmChannel = NotificationChannel(
            NotificationChannels.FCM_CHANNEL_ID,
            NotificationChannels.FCM_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifikasi tugas baru dari admin"
        }

        manager.createNotificationChannel(gpsChannel)
        manager.createNotificationChannel(fcmChannel)
    }
}
