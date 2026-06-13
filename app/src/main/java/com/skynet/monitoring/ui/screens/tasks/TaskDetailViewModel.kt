package com.skynet.monitoring.ui.screens.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.api.model.StatusAction
import com.skynet.monitoring.data.api.model.Task
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

/** Event sekali-pakai layar Detail Tugas. */
sealed interface TaskDetailEvent {
    /** Status berhasil diubah ke in_progress → navigasi ke layar Working. */
    data object RepairStarted : TaskDetailEvent
    data class ShowError(val message: String) : TaskDetailEvent
}

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<TaskDetailEvent>() {

    val taskId: Int = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow<UiState<Task>>(UiState.Loading)
    val uiState: StateFlow<UiState<Task>> = _uiState.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    init {
        load()
    }

    fun load() = _uiState.collectResult(viewModelScope) { taskRepository.getTaskDetail(taskId) }

    /** Tombol "Mulai Memperbaiki": kirim in_progress lalu picu navigasi ke Working. */
    fun startRepair() {
        viewModelScope.launch {
            _isUpdating.value = true
            taskRepository.updateStatus(taskId, StatusAction.START)
                .onSuccess {
                    // Navigasi ke Working; LocationService di-start di sana setelah izin lokasi.
                    emitEvent(TaskDetailEvent.RepairStarted)
                }
                .onFailure { emitEvent(TaskDetailEvent.ShowError(it.message ?: "Gagal memulai perbaikan")) }
            _isUpdating.value = false
        }
    }
}
