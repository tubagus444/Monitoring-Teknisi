package com.skynet.monitoring.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Badge status tugas. Memetakan nilai status backend ke label + warna.
 * Status yang mungkin: ditugaskan, sedang_memperbaiki, selesai.
 */
@Composable
fun StatusBadge(status: String) {
    val (label, container, content) = when (status) {
        "ditugaskan" -> Triple("Ditugaskan", Color(0xFFFFF3CD), Color(0xFF8A6D00))
        "sedang_memperbaiki" -> Triple("Sedang Memperbaiki", Color(0xFFCFE8FF), Color(0xFF0B4F8A))
        "selesai" -> Triple("Selesai", Color(0xFFD7F4DD), Color(0xFF1B5E20))
        else -> Triple(status, Color(0xFFE0E0E0), Color(0xFF424242))
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
