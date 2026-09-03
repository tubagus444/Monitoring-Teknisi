package com.skynet.monitoring.ui.screens.notifications

import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.api.model.NotificationItem
import com.skynet.monitoring.data.repository.NotificationRepository
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.ui.BaseViewModel
import com.skynet.monitoring.util.UiState
import com.skynet.monitoring.util.collectResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Event sekali-pakai untuk layar Notifikasi. */
sealed interface NotificationEvent {
    /** Navigasi ke detail tugas. [taskId] = ID assignment (bukan report). */
    data class NavigateToTask(val taskId: Int) : NotificationEvent
    /** Tampilkan toast pesan. */
    data class ShowToast(val message: String) : NotificationEvent
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val taskRepository: TaskRepository,
) : BaseViewModel<NotificationEvent>() {

    private val _uiState = MutableStateFlow<UiState<List<NotificationItem>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<NotificationItem>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
    }

    fun load() = _uiState.collectResult(viewModelScope) { notificationRepository.getNotifications() }

    fun refresh() =
        _uiState.collectResult(viewModelScope, _isRefreshing) { notificationRepository.getNotifications() }

    /** Tandai dibaca. Optimistic: langsung update list lokal, lalu sinkron ke server. */
    fun markRead(id: Int) {
        val current = _uiState.value
        if (current is UiState.Success) {
            val item = current.data.firstOrNull { it.id == id } ?: return
            if (item.isRead) return
            _uiState.value = UiState.Success(
                current.data.map { if (it.id == id) it.copy(isRead = true) else it },
            )
        }
        viewModelScope.launch {
            notificationRepository.markRead(id)
                .onFailure {
                    // Gagal — muat ulang agar state konsisten dengan server.
                    load()
                }
        }
    }

    /**
     * Buka notifikasi: tandai dibaca + navigasi ke tugas terkait.
     *
     * related_id dari backend = report_id (ID laporan), tapi navigasi Android
     * memakai task assignment ID. Kita cari assignment yang sesuai dari daftar
     * tugas aktif teknisi (GET /api/tasks → filter report_id == related_id).
     * Jika tidak ditemukan (tugas sudah selesai/dihapus), cukup tandai dibaca saja.
     */
    fun openNotification(id: Int) {
        val current = _uiState.value as? UiState.Success ?: return
        val item = current.data.firstOrNull { it.id == id } ?: return

        // Tandai dibaca (optimistic)
        markRead(id)

        val reportId = item.relatedId ?: return

        viewModelScope.launch {
            taskRepository.getTasks()
                .onSuccess { tasks ->
                    val task = tasks.firstOrNull { it.reportId == reportId }
                    if (task != null) {
                        emitEvent(NotificationEvent.NavigateToTask(task.id))
                    } else {
                        emitEvent(NotificationEvent.ShowToast("Tugas tidak ditemukan atau sudah selesai"))
                    }
                }
                .onFailure {
                    emitEvent(NotificationEvent.ShowToast("Gagal memuat tugas. Periksa koneksi Anda."))
                }
        }
    }
}
