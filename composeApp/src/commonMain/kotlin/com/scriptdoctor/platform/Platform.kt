package com.scriptdoctor.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Simple persistent string storage (SharedPreferences on Android). */
expect class KeyValueStore() {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}

expect fun nowEpochMs(): Long

/** Human-readable local date/time, e.g. "Nov 2, 2026, 3:15 PM". */
expect fun formatTimestamp(epochMs: Long): String

/** Decodes JPEG/PNG bytes for display. */
expect fun ByteArray.toImageBitmap(): ImageBitmap

class ImagePickerLauncher(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit,
)

/**
 * Returns launchers for the system photo picker and camera. Images arrive as JPEG bytes,
 * downscaled to at most [MAX_IMAGE_EDGE_PX] on the long edge and orientation-corrected.
 */
@Composable
expect fun rememberImagePicker(onImage: (ByteArray) -> Unit, onError: (String) -> Unit): ImagePickerLauncher

/** Claude's vision input gains nothing above ~1568 px on the long edge; larger just costs upload time. */
const val MAX_IMAGE_EDGE_PX = 1568
const val JPEG_QUALITY = 88
