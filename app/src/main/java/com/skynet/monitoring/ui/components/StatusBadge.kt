package com.skynet.monitoring.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skynet.monitoring.ui.theme.StatusColors

/**
 * Badge status tugas. Memetakan nilai status backend ke label + warna.
 * Warna diambil dari [StatusColors] dan menyesuaikan light/dark mode.
 * Status yang mungkin: ditugaskan, sedang_memperbaiki, selesai.
 */
@Composable
fun StatusBadge(status: String) {
    val dark = isSystemInDarkTheme()
    val (label, container, content) = when (status) {
        "ditugaskan" -> Triple(
            "Ditugaskan",
            if (dark) StatusColors.AssignedContainerDark else StatusColors.AssignedContainer,
            if (dark) StatusColors.AssignedContentDark else StatusColors.AssignedContent,
        )
        "sedang_memperbaiki" -> Triple(
            "Sedang Memperbaiki",
            if (dark) StatusColors.ProgressContainerDark else StatusColors.ProgressContainer,
            if (dark) StatusColors.ProgressContentDark else StatusColors.ProgressContent,
        )
        "selesai" -> Triple(
            "Selesai",
            if (dark) StatusColors.DoneContainerDark else StatusColors.DoneContainer,
            if (dark) StatusColors.DoneContentDark else StatusColors.DoneContent,
        )
        else -> Triple(
            status,
            if (dark) StatusColors.NeutralContainerDark else StatusColors.NeutralContainer,
            if (dark) StatusColors.NeutralContentDark else StatusColors.NeutralContent,
        )
    }

    Surface(color = container, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
