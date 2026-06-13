package com.skynet.monitoring.ui.screens.working

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.service.LocationService
import com.skynet.monitoring.ui.components.LoadingView
import com.skynet.monitoring.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingScreen(
    onFinished: () -> Unit,
    onMinimize: () -> Unit,
    viewModel: WorkingViewModel = hiltViewModel(),
) {
    val taskState by viewModel.task.collectAsStateWithLifecycle()
    val elapsed by viewModel.elapsedSeconds.collectAsStateWithLifecycle()
    val isFinishing by viewModel.isFinishing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // --- Runtime permission lokasi (+ notifikasi di Android 13+) ---
    var permissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(requiredPermissions())
    }

    // Mulai LocationService saat task termuat & permission diberikan.
    val reportId = (taskState as? UiState.Success)?.data?.reportId
    LaunchedEffect(reportId, permissionGranted) {
        if (reportId != null && reportId > 0 && permissionGranted) {
            LocationService.start(context, reportId)
        } else if (reportId != null && !permissionGranted) {
            snackbarHostState.showSnackbar("Izin lokasi diperlukan agar GPS bisa dikirim")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                WorkingEvent.Finished -> {
                    LocationService.stop(context)
                    onFinished()
                }
                is WorkingEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Tombol back sistem = kecilkan (kembali ke daftar tugas), BUKAN menyelesaikan tugas.
    // GPS foreground service sengaja dibiarkan jalan di belakang.
    BackHandler { onMinimize() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sedang Memperbaiki") },
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Kecilkan — GPS tetap berjalan di latar",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val state = taskState) {
            is UiState.Loading -> LoadingView(Modifier.padding(innerPadding))
            is UiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> WorkingContent(
                task = state.data,
                elapsed = elapsed,
                isFinishing = isFinishing,
                onFinish = viewModel::finishRepair,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun WorkingContent(
    task: Task,
    elapsed: Long,
    isFinishing: Boolean,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Status GPS aktif — diberi warna container brand agar status live menonjol.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column {
                    Text(
                        text = "GPS aktif",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Lokasi Anda dikirim ke admin secara berkala. Tetap berjalan " +
                            "walau layar ini dikecilkan (tombol ⌄ di atas).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Timer durasi
        Text(
            text = "Durasi Pengerjaan",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatDuration(elapsed),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = task.customer,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = task.address,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onFinish,
            enabled = !isFinishing,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isFinishing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
                Text("Menyelesaikan…")
            } else {
                Text("Tandai Selesai")
            }
        }
    }
}

/** Format detik → HH:MM:SS. */
private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
