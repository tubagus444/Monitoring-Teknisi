package com.skynet.monitoring.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Basis ViewModel dengan aliran event sekali-pakai ([events]) untuk aksi satu kali seperti
 * navigasi & menampilkan Snackbar.
 *
 * Menggantikan pola lama `MutableStateFlow<Boolean>` + `consumeX()` yang rawan ter-trigger
 * ganda saat recomposition. Layar mengonsumsi lewat satu kolektor:
 * ```
 * LaunchedEffect(Unit) { viewModel.events.collect { event -> ... } }
 * ```
 *
 * @param E tipe event layar (umumnya `sealed interface ...Event`).
 */
abstract class BaseViewModel<E> : ViewModel() {

    private val _events = Channel<E>(Channel.BUFFERED)

    /** Event sekali-pakai; tiap event dikonsumsi tepat sekali oleh kolektor di layar. */
    val events: Flow<E> = _events.receiveAsFlow()

    protected fun emitEvent(event: E) {
        viewModelScope.launch { _events.send(event) }
    }
}
