package com.skynet.monitoring.ui.screens.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val taskId: Int = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow<UiState<Task>>(UiState.Loading)
    val uiState: StateFlow<UiState<Task>> = _uiState.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    /** Event sekali-pakai: berhasil ubah ke in_progress → navigasi ke layar Working. */
    private val _repairStarted = MutableStateFlow(false)
    val repairStarted: StateFlow<Boolean> = _repairStarted.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            taskRepository.getTaskDetail(taskId)
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Gagal memuat detail tugas") }
        }
    }

    /** Tombol "Mulai Memperbaiki": kirim in_progress lalu picu navigasi ke Working. */
    fun startRepair() {
        viewModelScope.launch {
            _isUpdating.value = true
            taskRepository.updateStatus(taskId, "in_progress")
                .onSuccess {
                    // TODO(Fase 7): start LocationService di sini.
                    _repairStarted.value = true
                }
                .onFailure { _error.value = it.message ?: "Gagal memulai perbaikan" }
            _isUpdating.value = false
        }
    }

    fun consumeRepairStarted() { _repairStarted.value = false }
    fun consumeError() { _error.value = null }
}
