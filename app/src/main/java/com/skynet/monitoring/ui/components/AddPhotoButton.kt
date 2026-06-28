package com.skynet.monitoring.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/**
 * Tombol unggah foto yang membuka pilihan **Kamera** atau **Galeri**, lalu meneruskan [Uri]
 * hasilnya ke [onImagePicked] (caller yang mengompres & mengunggah).
 *
 * - Galeri: Android Photo Picker ([ActivityResultContracts.PickVisualMedia]) — tanpa izin runtime.
 * - Kamera: [ActivityResultContracts.TakePicture] menulis ke file cache via [FileProvider]; tidak
 *   mendeklarasikan permission `CAMERA` agar tak perlu prompt izin (pola resmi ACTION_IMAGE_CAPTURE).
 *
 * [enabled] di-set false selama proses unggah berlangsung untuk mencegah unggahan ganda.
 */
@Composable
fun AddPhotoButton(
    text: String,
    enabled: Boolean,
    onImagePicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showChooser by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) onImagePicked(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success -> if (success) pendingCameraUri?.let(onImagePicked) }

    OutlinedButton(
        onClick = { showChooser = true },
        enabled = enabled,
        modifier = modifier,
    ) {
        if (!enabled) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp,
            )
            Text("Mengunggah…")
        } else {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(text)
        }
    }

    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = { Text("Tambah Foto") },
            text = { Text("Ambil foto baru dengan kamera atau pilih dari galeri.") },
            confirmButton = {
                TextButton(onClick = {
                    showChooser = false
                    val uri = createCameraImageUri(context)
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                }) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Kamera")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChooser = false
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Galeri")
                }
            },
        )
    }
}

/** File cache + content-Uri tujuan tangkapan kamera (lewat FileProvider yang dideklarasikan di manifest). */
private fun createCameraImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
