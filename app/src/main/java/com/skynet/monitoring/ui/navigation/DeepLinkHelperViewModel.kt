package com.skynet.monitoring.ui.navigation

import androidx.lifecycle.ViewModel
import com.skynet.monitoring.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Lightweight ViewModel untuk menyediakan [TaskRepository] ke [AppNavGraph].
 *
 * Dibutuhkan karena AppNavGraph bukan Composable screen yang punya ViewModel sendiri,
 * tapi perlu akses repository untuk resolusi deep-link (report_id → task assignment ID).
 * Dibuat sekali (Activity-scoped via hiltViewModel) dan hanya menyediakan repository reference.
 */
@HiltViewModel
class DeepLinkHelperViewModel @Inject constructor(
    val taskRepository: TaskRepository,
) : ViewModel()
