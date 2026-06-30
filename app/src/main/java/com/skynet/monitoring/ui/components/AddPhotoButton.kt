package com.skynet.monitoring.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

/**
 * Tombol unggah foto yang membuka pilihan **Kamera** atau **Galeri**, lalu menampilkan **dialog
 * pratinjau** (thumbnail + keterangan opsional) sebelum benar-benar mengunggah. Baru saat user
 * menekan "Kirim", [onImagePicked] dipanggil dengan [Uri] + caption (null bila kosong) — caller
 * yang mengompres & mengunggah. Tombol "Batal" membuang foto tanpa mengunggah.
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
    onImagePicked: (Uri, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showChooser by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) previewUri = uri }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success -> if (success) previewUri = pendingCameraUri }

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

    previewUri?.let { uri ->
        PhotoPreviewDialog(
            uri = uri,
            onSend = { caption ->
                previewUri = null
                onImagePicked(uri, caption)
            },
            onCancel = { previewUri = null },
        )
    }
}

/**
 * Pratinjau sebelum kirim: thumbnail foto + kolom keterangan opsional. Mencegah salah foto
 * terlanjur terunggah (foto baru dikirim setelah ditekan "Kirim"). Caption kosong → dikirim `null`.
 */
@Composable
private fun PhotoPreviewDialog(
    uri: Uri,
    onSend: (String?) -> Unit,
    onCancel: () -> Unit,
) {
    var caption by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Kirim Foto?") },
        text = {
            Column {
                AsyncImage(
                    model = uri,
                    contentDescription = "Pratinjau foto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 255) caption = it },
                    label = { Text("Keterangan (opsional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(caption.trim().ifBlank { null }) }) {
                Text("Kirim")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Batal") }
        },
    )
}

/** File cache + content-Uri tujuan tangkapan kamera (lewat FileProvider yang dideklarasikan di manifest). */
private fun createCameraImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
