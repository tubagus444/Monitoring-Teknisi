package com.skynet.monitoring

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skynet.monitoring.service.MonitoringFirebaseService
import com.skynet.monitoring.ui.MainViewModel
import com.skynet.monitoring.ui.navigation.AppNavGraph
import com.skynet.monitoring.ui.theme.MonitoringTeknisiTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Deep-link: report_id dari FCM notifikasi (via tap notif saat background/killed).
     * Diekspos sebagai StateFlow agar AppNavGraph bisa membacanya sekali lalu
     * menavigasi ke tugas terkait.
     *
     * Nilai di-consume sekali (set ke null setelah dibaca) agar tidak trigger navigasi
     * ulang saat recomposition / configuration change.
     */
    private val _deepLinkReportId = MutableStateFlow<Int?>(null)
    val deepLinkReportId: StateFlow<Int?> = _deepLinkReportId.asStateFlow()

    fun consumeDeepLinkReportId(): Int? {
        val value = _deepLinkReportId.value
        _deepLinkReportId.value = null
        return value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLinkIntent(intent)
        enableEdgeToEdge()
        setContent {
            MonitoringTeknisiTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()

                when (val start = startDestination) {
                    null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }

                    else -> AppNavGraph(startDestination = start)
                }
            }
        }
    }

    /**
     * Dipanggil saat Activity sudah ada (singleTop) dan menerima intent baru
     * dari tap notifikasi. Misalnya app sudah di-foreground lalu user tap notifikasi
     * dari notification tray.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Coba baca dari tap notifikasi foreground (PendingIntent kita set sebagai Int)
        var relatedId = intent.getIntExtra(MonitoringFirebaseService.EXTRA_RELATED_ID, -1)

        // 2. Coba baca dari tap notifikasi background (Sistem FCM set data payload sebagai String)
        if (relatedId <= 0) {
            val fcmRelatedIdStr = intent.getStringExtra("related_id")
            relatedId = fcmRelatedIdStr?.toIntOrNull() ?: -1
        }

        if (relatedId > 0) {
            _deepLinkReportId.value = relatedId
            // Hapus extras agar tidak tertrigger ulang saat rotasi layar (recreation)
            intent.removeExtra(MonitoringFirebaseService.EXTRA_RELATED_ID)
            intent.removeExtra("related_id")
        }
    }
}