package com.skynet.monitoring.ui.screens.tasks

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.api.model.TaskCategory
import com.skynet.monitoring.data.api.model.TaskStatus
import com.skynet.monitoring.data.api.model.WorkLog
import com.skynet.monitoring.ui.components.CategoryBadge
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TaskDetailEvent.RepairStarted -> onNavigateToWorking(viewModel.taskId)
                is TaskDetailEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
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
                        text = task.displayTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    StatusBadge(task.status)
                }

                CategoryBadge(task.category, modifier = Modifier.padding(top = 8.dp))

                val isCustomer = TaskCategory.from(task.category) == TaskCategory.PELANGGAN
                InfoRow(label = "Jenis Kerusakan", value = task.damageType)
                InfoRow(
                    label = if (isCustomer) "Alamat" else "Lokasi/Area",
                    value = task.address,
                )
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

        // --- Kartu kontak & galeri foto: hanya untuk tugas kategori "pelanggan" ---
        if (TaskCategory.from(task.category) == TaskCategory.PELANGGAN) {
            CustomerContactCard(task = task, modifier = Modifier.padding(top = 16.dp))
            if (task.housePhotos.isNotEmpty()) {
                HousePhotoGallery(
                    photos = task.housePhotos,
                    modifier = Modifier.padding(top = 16.dp),
                )
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
        when (TaskStatus.from(task.status)) {
            TaskStatus.ASSIGNED -> Button(
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

            TaskStatus.IN_PROGRESS -> Button(
                onClick = onContinueRepair,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text("Lanjutkan Perbaikan")
            }

            // DONE / status tak dikenal → tugas tak muncul di list aktif, tak ada tombol aksi.
            else -> Unit
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

/**
 * Kartu kontak pelanggan: nama, no. HP (tombol Telepon + WhatsApp), IP, paket langganan.
 * Setiap baris hanya tampil bila datanya ada (semua field pelanggan bisa null).
 */
@Composable
private fun CustomerContactCard(task: Task, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Kontak Pelanggan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            task.customer?.let { InfoRow(label = "Nama", value = it) }
            task.ipAddress?.let { InfoRow(label = "IP Address", value = it) }
            task.subscriptionPackage?.let { InfoRow(label = "Paket Langganan", value = it) }

            task.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                InfoRow(label = "No. HP", value = phone)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("Telepon")
                    }
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("https://wa.me/${toWhatsAppNumber(phone)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("WhatsApp")
                    }
                }
            }
        }
    }
}

/**
 * Galeri foto rumah pelanggan. Thumbnail horizontal (Coil) dari URL absolut di [photos];
 * ketuk thumbnail untuk memperbesar dalam dialog. URL dimuat apa adanya (ikut host API).
 */
@Composable
private fun HousePhotoGallery(photos: List<String>, modifier: Modifier = Modifier) {
    var enlarged by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Foto Rumah",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos.size) { index ->
                val url = photos[index]
                AsyncImage(
                    model = url,
                    contentDescription = "Foto rumah ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { enlarged = url },
                )
            }
        }
    }

    enlarged?.let { url ->
        Dialog(onDismissRequest = { enlarged = null }) {
            AsyncImage(
                model = url,
                contentDescription = "Foto rumah diperbesar",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { enlarged = null },
            )
        }
    }
}

/**
 * Normalkan nomor HP Indonesia ke format wa.me (kode negara, tanpa "+"/0 di depan & non-digit).
 * Contoh: "0812-3456-7890" → "6281234567890".
 */
private fun toWhatsAppNumber(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.startsWith("0") -> "62" + digits.drop(1)
        digits.startsWith("62") -> digits
        else -> digits
    }
}
