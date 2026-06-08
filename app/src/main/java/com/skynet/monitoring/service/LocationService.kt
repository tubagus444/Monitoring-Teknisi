package com.skynet.monitoring.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.skynet.monitoring.data.repository.LocationRepository
import com.skynet.monitoring.util.Constants
import com.skynet.monitoring.util.NotificationChannels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service pengiriman GPS. Aktif selama teknisi berstatus sedang_memperbaiki.
 * Mengirim koordinat ke /api/location tiap [Constants.LOCATION_INTERVAL_MS] dengan report_id aktif.
 */
@AndroidEntryPoint
class LocationService : Service() {

    @Inject lateinit var locationRepository: LocationRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var reportId: Int = -1

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                reportId = intent.getIntExtra(EXTRA_REPORT_ID, -1)
                startAsForeground()
                requestLocationUpdates()
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_INTERVAL_MS,
        ).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (reportId > 0) {
                    scope.launch {
                        locationRepository.sendLocation(
                            reportId = reportId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                        )
                    }
                }
            }
        }
        locationCallback = callback
        // Permission sudah dipastikan granted oleh WorkingScreen sebelum start.
        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, NotificationChannels.GPS_CHANNEL_ID)
            .setContentTitle("SkyNet — GPS aktif")
            .setContentText("Sedang memperbaiki • lokasi dikirim ke admin")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onDestroy() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_START = "com.skynet.monitoring.action.START_LOCATION"
        private const val ACTION_STOP = "com.skynet.monitoring.action.STOP_LOCATION"
        private const val EXTRA_REPORT_ID = "extra_report_id"
        private const val NOTIFICATION_ID = 1001

        /** Mulai service untuk report_id tertentu. Pastikan permission lokasi sudah granted. */
        fun start(context: Context, reportId: Int) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_REPORT_ID, reportId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
