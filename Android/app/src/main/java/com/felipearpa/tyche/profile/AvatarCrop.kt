package com.felipearpa.tyche.profile

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Pure geometry for the avatar crop screen, shared as a spec with the iOS implementation:
 * `scale` maps source pixels to view pixels, the crop window is a centered square of
 * [windowSide], and `offset` is the translation of the image center from the window center.
 */
data class AvatarCrop(val imageSize: Size, val windowSide: Float) {

    /** The image always covers the window (aspect fill), never less. */
    val minScale: Float
        get() = windowSide / minOf(imageSize.width, imageSize.height)

    /**
     * The visible region never holds fewer source pixels than the output needs,
     * capped at 5× the cover zoom; tiny sources collapse the range to [minScale].
     */
    val maxScale: Float
        get() {
            val qualityZoomFactor = minOf(imageSize.width, imageSize.height) / OUTPUT_SIDE
            return minScale * qualityZoomFactor.coerceIn(1f, MAX_ZOOM_FACTOR)
        }

    fun clampScale(scale: Float): Float = scale.coerceIn(minScale, maxScale)

    fun clampOffset(offset: Offset, scale: Float): Offset {
        val clampedScale = clampScale(scale)
        val horizontalSlack = ((imageSize.width * clampedScale - windowSide) / 2f).coerceAtLeast(0f)
        val verticalSlack = ((imageSize.height * clampedScale - windowSide) / 2f).coerceAtLeast(0f)

        return Offset(
            offset.x.coerceIn(-horizontalSlack, horizontalSlack),
            offset.y.coerceIn(-verticalSlack, verticalSlack),
        )
    }

    /**
     * Offset that keeps the image point under [anchor] (window coordinates, relative to the
     * window center) stationary while zooming, re-clamped so the window stays covered.
     */
    fun offset(zoomingTo: Float, from: Float, offset: Offset, anchor: Offset): Offset {
        val clampedScale = clampScale(zoomingTo)
        val zoomRatio = clampedScale / from

        val zoomedOffset = Offset(
            anchor.x - (anchor.x - offset.x) * zoomRatio,
            anchor.y - (anchor.y - offset.y) * zoomRatio,
        )

        return clampOffset(zoomedOffset, clampedScale)
    }

    /** The visible square under the window, in source pixels. */
    fun cropRect(scale: Float, offset: Offset): Rect {
        val side = windowSide / scale

        return Rect(
            offset = Offset(
                imageSize.width / 2f - (windowSide / 2f + offset.x) / scale,
                imageSize.height / 2f - (windowSide / 2f + offset.y) / scale,
            ),
            size = Size(side, side),
        )
    }

    companion object {
        const val OUTPUT_SIDE = 512f
        const val MAX_ZOOM_FACTOR = 5f
    }
}
