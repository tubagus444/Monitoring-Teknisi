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
 * - [onMessageReceived] hanya dipanggil saat app **foreground** (pesan notification+data).
 *   Saat background/killed, sistem menampilkan notif otomatis dari blok `notification` —
 *   service ini tidak dipanggil. Deep-link dari background ditangani via `intent.extras`
 *   di [MainActivity].
 * - [onNewToken] dipanggil saat token perangkat berubah → kirim ulang ke backend bila sudah login.
 *
 * Backend kini menyertakan blok `data` (`type`, `related_id`) untuk deep-link.
 * Saat foreground, kita baca `data` dan bangun `PendingIntent` dengan extras agar
 * tap notifikasi membuka detail tugas terkait.
 */
@AndroidEntryPoint
class MonitoringFirebaseService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        // Backend mengirim notification+data; ambil title/body dari blok notification.
        val title = message.notification?.title ?: "Notifikasi Baru"
        val body = message.notification?.body ?: ""

        // Deep-link: baca related_id dari blok data (selalu string di FCM → parse ke Int).
        val relatedId = message.data["related_id"]?.toIntOrNull()

        showNotification(title, body, relatedId)
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

    private fun showNotification(title: String, body: String, relatedId: Int?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Sertakan related_id agar MainActivity bisa menavigasi ke tugas terkait.
            if (relatedId != null) {
                putExtra(EXTRA_RELATED_ID, relatedId)
            }
        }

        // requestCode unik per notifikasi agar PendingIntent dengan extras berbeda
        // tidak saling menimpa (FLAG_UPDATE_CURRENT menimpa extras yang sama).
        val requestCode = nextNotificationId()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
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

        // Cek permission Android 13+ agar Lint tidak error (walau notify() aman jika ditolak).
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        NotificationManagerCompat.from(this).notify(requestCode, notification)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Key intent extra untuk report_id dari notifikasi FCM. */
        const val EXTRA_RELATED_ID = "fcm_related_id"

        private val notificationCounter = AtomicInteger(2000)

        /** ID unik agar tiap notif tampil terpisah (dimulai dari 2000 agar tak bentrok GPS=1001). */
        private fun nextNotificationId(): Int = notificationCounter.incrementAndGet()
    }
}
