package com.skynet.monitoring.ui.screens.working

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.api.model.StatusAction
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.api.model.TaskStatus
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.ui.BaseViewModel
import com.skynet.monitoring.util.DateUtils
import com.skynet.monitoring.util.UiState
import com.skynet.monitoring.util.collectResult
import com.skynet.monitoring.util.elapsedSecondsFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Event sekali-pakai layar Sedang Memperbaiki. */
sealed interface WorkingEvent {
    /** Status berhasil diubah ke done → stop GPS & kembali ke daftar tugas. */
    data object Finished : WorkingEvent
    data class ShowMessage(val message: String) : WorkingEvent
    data class ShowError(val message: String) : WorkingEvent
}

@HiltViewModel
class WorkingViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<WorkingEvent>() {

    val taskId: Int = checkNotNull(savedStateHandle["id"])

    private val _task = MutableStateFlow<UiState<Task>>(UiState.Loading)
    val task: StateFlow<UiState<Task>> = _task.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _isFinishing = MutableStateFlow(false)
    val isFinishing: StateFlow<Boolean> = _isFinishing.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    init {
        load()
        startDurationTimer()
        // LocationService di-start oleh WorkingScreen saat task termuat & izin lokasi granted.
    }

    private fun load() = _task.collectResult(viewModelScope) { taskRepository.getTaskDetail(taskId) }

    /**
     * Menyalurkan durasi kerja dari [elapsedSecondsFlow] ke [elapsedSeconds]. Waktu mulai di-anchor
     * ke `work_log` "sedang_memperbaiki" (waktu perbaikan dimulai) — agar timer tetap akurat walau
     * layar Working dibuka ulang setelah dikecilkan (VM dibuat ulang). Bila tak ada/parse gagal,
     * timer mulai dari sekarang (0). Perhitungan detik & tick dilakukan di util, bukan di VM.
     */
    private fun startDurationTimer() {
        viewModelScope.launch {
            val task = _task.first { it is UiState.Success } as UiState.Success
            val startMillis = task.data.workLogs
                ?.lastOrNull { it.status == TaskStatus.IN_PROGRESS.apiValue }
                ?.let { DateUtils.toEpochMillis(it.loggedAt) }
            elapsedSecondsFlow(startMillis).collect { _elapsedSeconds.value = it }
        }
    }

    /**
     * Tombol "Tandai Selesai": kirim done (beserta [note] catatan pekerjaan opsional), lalu picu
     * stop GPS & kembali ke daftar tugas.
     */
    fun finishRepair(note: String? = null) {
        viewModelScope.launch {
            _isFinishing.value = true
            taskRepository.updateStatus(taskId, StatusAction.FINISH, note)
                .onSuccess { emitEvent(WorkingEvent.Finished) }
                .onFailure { emitEvent(WorkingEvent.ShowError(it.message ?: "Gagal menyelesaikan tugas")) }
            _isFinishing.value = false
        }
    }

    /** Unggah foto bukti pekerjaan dari [uri] (kamera/galeri), lalu muat ulang detail. */
    fun uploadRepairPhoto(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            taskRepository.uploadRepairPhoto(taskId, uri)
                .onSuccess {
                    emitEvent(WorkingEvent.ShowMessage("Foto bukti diunggah"))
                    load()
                }
                .onFailure { emitEvent(WorkingEvent.ShowError(it.message ?: "Gagal mengunggah foto")) }
            _isUploading.value = false
        }
    }
}
