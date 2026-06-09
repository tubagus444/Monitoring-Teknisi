package com.skynet.monitoring.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet warna brand SkyNet. Satu sumber kebenaran untuk seluruh app.
 * Aksen utama: biru (kesan teknis & terpercaya untuk ISP/jaringan).
 *
 * Mengikuti aturan 60-30-10:
 *  - 60% background/surface (netral terang/gelap)
 *  - 30% teks & elemen struktural (onSurface)
 *  - 10% aksen biru (primary) untuk aksi penting
 */

val Color_White = Color(0xFFFFFFFF)

// --- Brand biru ---
val SkyBlue = Color(0xFF0B5FB0)          // primary (light)
val SkyBlueDark = Color(0xFF003C73)      // untuk teks di atas container terang
val SkyBlueContainer = Color(0xFFD4E3FF) // container biru muda
val SkyBlue80 = Color(0xFFA5C8FF)        // primary (dark mode)

// --- Netral (light) ---
val Surface = Color(0xFFFDFCFF)
val SurfaceVariant = Color(0xFFE1E2EC)
val OnSurface = Color(0xFF1A1C1E)
val OnSurfaceVariant = Color(0xFF44474E)
val Outline = Color(0xFF74777F)

// --- Netral (dark) ---
val SurfaceDark = Color(0xFF121316)
val SurfaceVariantDark = Color(0xFF44474E)
val OnSurfaceDark = Color(0xFFE3E2E6)
val OnSurfaceVariantDark = Color(0xFFC4C6CF)
val OutlineDark = Color(0xFF8E9099)

// --- Error ---
val ErrorRed = Color(0xFFBA1A1A)
val ErrorRed80 = Color(0xFFFFB4AB)

// --- Warna status tugas (badge). Pasangan container/content per mode. ---
// Ditugaskan → amber, Sedang memperbaiki → biru, Selesai → hijau.
object StatusColors {
    // Light
    val AssignedContainer = Color(0xFFFFF1C2)
    val AssignedContent = Color(0xFF6B5300)
    val ProgressContainer = Color(0xFFD4E3FF)
    val ProgressContent = Color(0xFF003C73)
    val DoneContainer = Color(0xFFC8F0CF)
    val DoneContent = Color(0xFF14512A)
    val NeutralContainer = Color(0xFFE1E2EC)
    val NeutralContent = Color(0xFF44474E)

    // Dark (container lebih gelap, content lebih terang agar kontras aman)
    val AssignedContainerDark = Color(0xFF534600)
    val AssignedContentDark = Color(0xFFFFE08B)
    val ProgressContainerDark = Color(0xFF00497F)
    val ProgressContentDark = Color(0xFFD4E3FF)
    val DoneContainerDark = Color(0xFF1F5134)
    val DoneContentDark = Color(0xFFC8F0CF)
    val NeutralContainerDark = Color(0xFF44474E)
    val NeutralContentDark = Color(0xFFE1E2EC)
}
