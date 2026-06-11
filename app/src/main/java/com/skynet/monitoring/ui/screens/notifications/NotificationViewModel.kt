package com.skynet.monitoring.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skynet.monitoring.data.api.model.NotificationItem
import com.skynet.monitoring.data.repository.NotificationRepository
import com.skynet.monitoring.util.UiState
import com.skynet.monitoring.util.collectResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

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
}
