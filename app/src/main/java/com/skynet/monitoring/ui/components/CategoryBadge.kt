package com.skynet.monitoring.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skynet.monitoring.data.api.model.TaskCategory

/**
 * Badge kategori tugas (Gangguan Pelanggan / Jaringan / Pemeliharaan). Memakai warna
 * secondaryContainer agar berbeda dari [StatusBadge]. Tidak tampil bila kategori tak dikenal.
 */
@Composable
fun CategoryBadge(category: String?, modifier: Modifier = Modifier) {
    val label = TaskCategory.from(category)?.label ?: return
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
