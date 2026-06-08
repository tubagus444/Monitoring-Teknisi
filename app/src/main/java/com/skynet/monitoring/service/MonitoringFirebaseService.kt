package com.skynet.monitoring.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.skynet.monitoring.MainActivity
import com.skynet.monitoring.R
import com.skynet.monitoring.data.repository.AuthRepository
import com.skynet.monitoring.util.NotificationChannels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Penerima pesan FCM.
 *
 * - [onMessageReceived] hanya dipanggil saat app **foreground** (pesan notification-only).
 *   Saat background/killed, sistem menampilkan notif otomatis dari blok `notification` —
 *   service ini tidak dipanggil. Maka di sini kita tampilkan notif manual via NotificationManager.
 * - [onNewToken] dipanggil saat token perangkat berubah → kirim ulang ke backend bila sudah login.
 *
 * Deep-link sengaja TIDAK dikerjakan (sesuai CLAUDE.md): tap notif cukup membuka app di halaman awal.
 */
@AndroidEntryPoint
class MonitoringFirebaseService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        // Backend mengirim notification-only; ambil title/body dari blok notification.
        val title = message.notification?.title ?: "Notifikasi Baru"
        val body = message.notification?.body ?: ""
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        // Hanya kirim bila ada sesi login aktif (token lokal tersedia).
        scope.launch {
            val loggedIn = authRepository.tokenFlow.first()?.isNotBlank() == true
            if (loggedIn) {
                authRepository.updateFcmToken(token)
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        // Tap → buka MainActivity di halaman awal (Daftar Tugas). Tanpa deep-link.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.FCM_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // notify() no-op diam-diam bila POST_NOTIFICATIONS belum diberi (Android 13+) — aman.
        NotificationManagerCompat.from(this).notify(nextNotificationId(), notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private val notificationCounter = AtomicInteger(2000)

        /** ID unik agar tiap notif tampil terpisah (dimulai dari 2000 agar tak bentrok GPS=1001). */
        private fun nextNotificationId(): Int = notificationCounter.incrementAndGet()
    }
}
