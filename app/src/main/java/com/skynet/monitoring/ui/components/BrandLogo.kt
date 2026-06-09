package com.skynet.monitoring.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Logo mark SkyNet: ikon antena sinyal di dalam lingkaran brand.
 *
 * Placeholder yang disengaja (bukan file logo). Saat logo asli sudah ada,
 * cukup ganti isi Composable ini dengan `Image(painterResource(...))` —
 * semua layar pemakai (Login, dsb.) ikut berubah tanpa disentuh.
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.SettingsInputAntenna,
                contentDescription = "Logo SkyNet",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}
