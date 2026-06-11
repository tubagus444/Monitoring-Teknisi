package com.skynet.monitoring.ui.screens.tasks

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.api.model.WorkLog
import com.skynet.monitoring.ui.components.ErrorView
import com.skynet.monitoring.ui.components.LoadingView
import com.skynet.monitoring.ui.components.StatusBadge
import com.skynet.monitoring.util.DateUtils
import com.skynet.monitoring.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onBack: () -> Unit,
    onNavigateToWorking: (Int) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isUpdating by viewModel.isUpdating.collectAsStateWithLifecycle()
    val repairStarted by viewModel.repairStarted.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(repairStarted) {
        if (repairStarted) {
            viewModel.consumeRepairStarted()
            onNavigateToWorking(viewModel.taskId)
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Tugas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val state = uiState) {
            is UiState.Loading -> LoadingView(Modifier.padding(innerPadding))
            is UiState.Error -> ErrorView(
                state.message,
                modifier = Modifier.padding(innerPadding),
                onRetry = viewModel::load,
            )
            is UiState.Success -> DetailContent(
                task = state.data,
                isUpdating = isUpdating,
                onStartRepair = viewModel::startRepair,
                onContinueRepair = { onNavigateToWorking(viewModel.taskId) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun DetailContent(
    task: Task,
    isUpdating: Boolean,
    onStartRepair: () -> Unit,
    onContinueRepair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // --- Info utama (dikelompokkan dalam satu kartu) ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = task.customer,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    StatusBadge(task.status)
                }

                InfoRow(label = "Jenis Kerusakan", value = task.damageType)
                InfoRow(label = "Alamat", value = task.address)
                InfoRow(label = "Catatan", value = task.notes ?: "-")
                InfoRow(label = "Ditugaskan", value = DateUtils.format(task.assignedAt))

                OutlinedButton(
                    onClick = {
                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(task.address)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Buka di Google Maps")
                }
            }
        }

        // --- Timeline work logs ---
        if (!task.workLogs.isNullOrEmpty()) {
            Text(
                text = "Riwayat Pengerjaan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    task.workLogs.forEachIndexed { index, log ->
                        WorkLogItem(log)
                        if (index < task.workLogs.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }

        // --- Tombol aksi ---
        when (task.status) {
            "ditugaskan" -> Button(
                onClick = onStartRepair,
                enabled = !isUpdating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Memproses…")
                } else {
                    Text("Mulai Memperbaiki")
                }
            }

            "sedang_memperbaiki" -> Button(
                onClick = onContinueRepair,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text("Lanjutkan Perbaikan")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WorkLogItem(log: WorkLog) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatusBadge(log.status)
            Text(
                text = DateUtils.format(log.loggedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        log.technician?.let {
            Text(
                text = "Teknisi: $it",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        log.description?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
    }
}
