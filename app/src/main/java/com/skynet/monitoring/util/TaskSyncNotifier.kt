package com.skynet.monitoring.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bus event sederhana untuk sinkronisasi data antar-komponen aplikasi.
 *
 * Digunakan oleh [com.skynet.monitoring.service.MonitoringFirebaseService] saat notifikasi FCM
 * diterima di foreground untuk memicu refresh otomatis pada [TaskListViewModel] dan
 * [NotificationViewModel] tanpa mengharuskan teknisi berpindah halaman terlebih dahulu.
 */
@Singleton
class TaskSyncNotifier @Inject constructor() {

    private val _syncEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val syncEvents: SharedFlow<Unit> = _syncEvents.asSharedFlow()

    fun notifyTaskUpdate() {
        _syncEvents.tryEmit(Unit)
    }
}
