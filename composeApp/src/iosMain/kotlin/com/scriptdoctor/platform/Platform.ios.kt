@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.scriptdoctor.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

actual class KeyValueStore actual constructor() {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String): String? = defaults.stringForKey(key)

    actual fun putString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, forKey = key)
    }
}

actual fun nowEpochMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatTimestamp(epochMs: Long): String {
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0))
}

actual fun ByteArray.toImageBitmap(): ImageBitmap = Image.makeFromEncoded(this).toComposeImageBitmap()

@Composable
actual fun rememberImagePicker(onImage: (ByteArray) -> Unit, onError: (String) -> Unit): ImagePickerLauncher {
    val currentOnImage = rememberUpdatedState(onImage)
    val currentOnError = rememberUpdatedState(onError)
    val deliver: (ByteArray?) -> Unit = { bytes ->
        dispatch_async(dispatch_get_main_queue()) {
            if (bytes != null) currentOnImage.value(bytes) else currentOnError.value("Couldn't read that image.")
        }
    }
    // Delegates are weakly referenced by UIKit, so keep them alive for the composition's lifetime.
    val galleryDelegate = remember { GalleryDelegate(deliver) }
    val cameraDelegate = remember { CameraDelegate(deliver) }

    return remember {
        ImagePickerLauncher(
            pickFromGallery = {
                val config = PHPickerConfiguration().apply {
                    filter = PHPickerFilter.imagesFilter
                    selectionLimit = 1
                }
                val picker = PHPickerViewController(configuration = config)
                picker.delegate = galleryDelegate
                topViewController()?.presentViewController(picker, animated = true, completion = null)
            },
            takePhoto = {
                val source = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                if (!UIImagePickerController.isSourceTypeAvailable(source)) {
                    currentOnError.value("No camera available on this device.")
                } else {
                    val picker = UIImagePickerController()
                    picker.sourceType = source
                    picker.delegate = cameraDelegate
                    topViewController()?.presentViewController(picker, animated = true, completion = null)
                }
            },
        )
    }
}

private class GalleryDelegate(private val deliver: (ByteArray?) -> Unit) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
        result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            val image = data?.let { UIImage(data = it) }
            deliver(image?.let(::compress))
        }
    }
}

private class CameraDelegate(private val deliver: (ByteArray?) -> Unit) :
    NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        deliver(image?.let(::compress))
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

private fun topViewController(): UIViewController? {
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

/** Redraws into a bitmap context (which also bakes in the EXIF orientation), downscales, and encodes JPEG. */
private fun compress(image: UIImage): ByteArray? {
    val (w, h) = image.size.useContents { width to height }
    if (w <= 0.0 || h <= 0.0) return null
    val scale = minOf(1.0, MAX_IMAGE_EDGE_PX / maxOf(w, h))
    val nw = (w * scale).toInt().toDouble()
    val nh = (h * scale).toInt().toDouble()
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(nw, nh), true, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, nw, nh))
    val resized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    val data = UIImageJPEGRepresentation(resized ?: return null, JPEG_QUALITY / 100.0) ?: return null
    return data.toByteArray()
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}
