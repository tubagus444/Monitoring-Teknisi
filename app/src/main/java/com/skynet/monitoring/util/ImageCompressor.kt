package com.skynet.monitoring.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Membaca gambar dari `content://` Uri (hasil kamera/galeri) lalu mengompresnya menjadi JPEG
 * < 5 MB agar lolos validasi backend (`POST /tasks/{id}/photos` & `/house-photos`, maks 5 MB).
 *
 * Langkah: down-sample saat decode (batasi dimensi terpanjang ~[MAX_DIMENSION] px) → koreksi
 * rotasi EXIF → turunkan kualitas JPEG bertahap sampai di bawah ambang. Semua di [Dispatchers.IO].
 */
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * @return [MultipartBody.Part] bernama `photo` siap dikirim Retrofit, atau `null` bila Uri
     * tidak bisa dibaca/di-decode (mis. file rusak atau bukan gambar).
     */
    suspend fun toPhotoPart(uri: Uri): MultipartBody.Part? = withContext(Dispatchers.IO) {
        val bitmap = decodeSampled(uri) ?: return@withContext null
        val rotated = applyExifRotation(uri, bitmap)
        val bytes = compressUnderLimit(rotated)
        rotated.recycle()
        val body = bytes.toRequestBody(JPEG_MIME.toMediaType())
        MultipartBody.Part.createFormData("photo", "upload_${System.currentTimeMillis()}.jpg", body)
    }

    /** Decode dengan [BitmapFactory.Options.inSampleSize] agar bitmap tak membebani memori. */
    private fun decodeSampled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / sample > MAX_DIMENSION) sample *= 2
        return sample
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it != bitmap) bitmap.recycle() }
    }

    /** Turunkan kualitas JPEG bertahap hingga hasil < [MAX_BYTES] atau mencapai [MIN_QUALITY]. */
    private fun compressUnderLimit(bitmap: Bitmap): ByteArray {
        var quality = START_QUALITY
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= QUALITY_STEP
        } while (bytes.size > MAX_BYTES && quality >= MIN_QUALITY)
        return bytes
    }

    private companion object {
        const val JPEG_MIME = "image/jpeg"
        const val MAX_DIMENSION = 1600
        const val MAX_BYTES = 5 * 1024 * 1024 - 256 * 1024 // sisakan margin dari batas 5 MB backend
        const val START_QUALITY = 90
        const val MIN_QUALITY = 50
        const val QUALITY_STEP = 10
    }
}
