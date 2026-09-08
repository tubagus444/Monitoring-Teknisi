package com.skynet.monitoring.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Memuat data ber-[Result] ke dalam [UiState] dengan pola standar aplikasi, menggantikan
 * boilerplate `load()`/`refresh()` yang berulang di banyak ViewModel.
 *
 * - **Mode normal** (`refreshing == null`): set [UiState.Loading] dulu, lalu Success/Error.
 * - **Mode refresh** (`refreshing != null`): nyalakan flag refresh selama proses; pada error,
 *   pertahankan data lama bila sudah ada (hanya tampilkan [UiState.Error] bila belum ada Success).
 *
 * Pesan error diambil dari `Throwable.message` ([ApiException] selalu mengisinya).
 */
fun <T> MutableStateFlow<UiState<T>>.collectResult(
    scope: CoroutineScope,
    refreshing: MutableStateFlow<Boolean>? = null,
    block: suspend () -> Result<T>,
) {
    scope.launch {
        if (refreshing != null) refreshing.value = true else value = UiState.Loading
        runCatching { block() }
            .onSuccess { result ->
                result
                    .onSuccess { value = UiState.Success(it) }
                    .onFailure {
                        if (value !is UiState.Success) {
                            value = UiState.Error(it.message ?: "Terjadi kesalahan")
                        }
                    }
            }
            .onFailure { error ->
                if (value !is UiState.Success) {
                    value = UiState.Error(error.message ?: "Terjadi kesalahan")
                }
            }
        refreshing?.value = false
    }
}
