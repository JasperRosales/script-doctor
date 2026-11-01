package com.scriptdoctor.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.scriptdoctor.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.DateFormat
import java.util.Date

actual class KeyValueStore actual constructor() {
    private val prefs = AndroidAppContext.context.getSharedPreferences("script_doctor", Context.MODE_PRIVATE)

    actual fun getString(key: String): String? = prefs.getString(key, null)

    actual fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}

actual fun nowEpochMs(): Long = System.currentTimeMillis()

actual fun formatTimestamp(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))

actual fun ByteArray.toImageBitmap(): ImageBitmap =
    BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()

@Composable
actual fun rememberImagePicker(onImage: (ByteArray) -> Unit, onError: (String) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnImage = rememberUpdatedState(onImage)
    val currentOnError = rememberUpdatedState(onError)
    // Survives process death while the camera app is in the foreground.
    val pendingCapture = rememberSaveable { arrayOf<String?>(null) }

    fun process(uri: Uri) {
        scope.launch {
            val bytes = withContext(Dispatchers.IO) { runCatching { loadAndCompress(context, uri) }.getOrNull() }
            if (bytes != null) currentOnImage.value(bytes) else currentOnError.value("Couldn't read that image.")
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) process(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCapture[0]?.let(Uri::parse)
        if (ok && uri != null) process(uri)
    }

    return remember {
        ImagePickerLauncher(
            pickFromGallery = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            takePhoto = {
                val dir = File(context.cacheDir, "captures").apply { mkdirs() }
                val file = File(dir, "capture.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                pendingCapture[0] = uri.toString()
                runCatching { camera.launch(uri) }.onFailure { currentOnError.value("No camera app available.") }
            },
        )
    }
}

/** Decodes with subsampling, applies EXIF rotation, scales to [MAX_IMAGE_EDGE_PX] and re-encodes as JPEG. */
private fun loadAndCompress(context: Context, uri: Uri): ByteArray {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_EDGE_PX) sample *= 2

    val decoded = resolver.openInputStream(uri)!!.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: error("decode failed")

    val rotation = resolver.openInputStream(uri)!!.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }
    val longEdge = maxOf(decoded.width, decoded.height)
    val scale = if (longEdge > MAX_IMAGE_EDGE_PX) MAX_IMAGE_EDGE_PX.toFloat() / longEdge else 1f
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(rotation)
    }
    val out: Bitmap = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    return ByteArrayOutputStream().use { stream ->
        out.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        stream.toByteArray()
    }
}
