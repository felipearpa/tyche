package com.felipearpa.tyche.profile

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import java.io.ByteArrayOutputStream

/**
 * Decodes with EXIF orientation applied — camera captures included — which is what the crop
 * geometry expects. Very large sources are downscaled at decode time to bound memory; the cap
 * stays far above the 512-pixel output times the 5× zoom budget.
 */
fun decodeOrientedBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap? =
    runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE

            val longestSide = maxOf(info.size.width, info.size.height)
            if (longestSide > MAX_DECODE_SIDE) {
                val downscale = MAX_DECODE_SIDE.toFloat() / longestSide
                decoder.setTargetSize(
                    (info.size.width * downscale).toInt(),
                    (info.size.height * downscale).toInt(),
                )
            }
        }
    }.getOrNull()

private const val MAX_DECODE_SIDE = 4096

/** Renders the crop-rect region into a square avatar and encodes it as JPEG. */
fun Bitmap.avatarRender(
    cropRect: Rect,
    outputSide: Int = AvatarCrop.OUTPUT_SIDE.toInt(),
    quality: Int = 80,
): Pair<Bitmap, ByteArray> {
    val output = Bitmap.createBitmap(outputSide, outputSide, Bitmap.Config.ARGB_8888)
    val renderScale = outputSide / cropRect.width

    val matrix = Matrix().apply {
        postTranslate(-cropRect.left, -cropRect.top)
        postScale(renderScale, renderScale)
    }

    Canvas(output).drawBitmap(this, matrix, Paint(Paint.FILTER_BITMAP_FLAG))

    val stream = ByteArrayOutputStream()
    output.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return output to stream.toByteArray()
}
