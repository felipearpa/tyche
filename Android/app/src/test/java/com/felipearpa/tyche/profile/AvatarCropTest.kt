package com.felipearpa.tyche.profile

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AvatarCropTest {

    @Test
    fun `given a portrait source when the minimum scale is computed then the image exactly covers the window`() {
        val crop = AvatarCrop(imageSize = Size(1000f, 2000f), windowSide = 300f)

        crop.minScale shouldBe (0.3f plusOrMinus EPSILON)
    }

    @Test
    fun `given a landscape source when the minimum scale is computed then the image exactly covers the window`() {
        val crop = AvatarCrop(imageSize = Size(2000f, 1000f), windowSide = 300f)

        crop.minScale shouldBe (0.3f plusOrMinus EPSILON)
    }

    @Test
    fun `given a square source when the minimum scale is computed then the image exactly covers the window`() {
        val crop = AvatarCrop(imageSize = Size(1000f, 1000f), windowSide = 300f)

        crop.minScale shouldBe (0.3f plusOrMinus EPSILON)
    }

    @Test
    fun `given a very large source when the maximum scale is computed then it caps at five times the cover zoom`() {
        val crop = AvatarCrop(imageSize = Size(4000f, 4000f), windowSide = 300f)

        crop.maxScale shouldBe (crop.minScale * 5f plusOrMinus EPSILON)
    }

    @Test
    fun `given a mid-size source when the maximum scale is computed then quality bounds it below the cap`() {
        val crop = AvatarCrop(imageSize = Size(1024f, 2048f), windowSide = 300f)

        crop.maxScale shouldBe (crop.minScale * 2f plusOrMinus EPSILON)
    }

    @Test
    fun `given a source smaller than the output when the maximum scale is computed then the minimum wins`() {
        val crop = AvatarCrop(imageSize = Size(400f, 600f), windowSide = 300f)

        crop.maxScale shouldBe (crop.minScale plusOrMinus EPSILON)
        crop.clampScale(10f) shouldBe (crop.minScale plusOrMinus EPSILON)
        crop.clampScale(0.001f) shouldBe (crop.minScale plusOrMinus EPSILON)
    }

    @Test
    fun `given offsets past each edge when clamped then the window stays covered on all four sides`() {
        val crop = AvatarCrop(imageSize = Size(1000f, 1000f), windowSide = 300f)
        val scale = 0.5f

        val pastTrailingBottom = crop.clampOffset(Offset(200f, 200f), scale)
        val pastLeadingTop = crop.clampOffset(Offset(-200f, -200f), scale)
        val within = crop.clampOffset(Offset(80f, -90f), scale)

        pastTrailingBottom.shouldBeApproximately(Offset(100f, 100f))
        pastLeadingTop.shouldBeApproximately(Offset(-100f, -100f))
        within.shouldBeApproximately(Offset(80f, -90f))
    }

    @Test
    fun `given the image exactly covers the window when any offset is clamped then it collapses to zero`() {
        val crop = AvatarCrop(imageSize = Size(1000f, 1000f), windowSide = 300f)

        val clamped = crop.clampOffset(Offset(10f, -10f), crop.minScale)

        clamped.shouldBeApproximately(Offset.Zero)
    }

    @Test
    fun `given a zoom about an anchor when the offset is recomputed then the anchored image point stays put`() {
        val crop = AvatarCrop(imageSize = Size(4000f, 4000f), windowSide = 300f)

        val offset = crop.offset(
            zoomingTo = 0.3f,
            from = 0.15f,
            offset = Offset(10f, 20f),
            anchor = Offset(50f, -30f),
        )

        offset.shouldBeApproximately(Offset(-30f, 70f))
    }

    @Test
    fun `given a zoom out to the cover minimum when the offset is recomputed then it re-clamps to keep coverage`() {
        val crop = AvatarCrop(imageSize = Size(1000f, 1000f), windowSide = 300f)

        val offset = crop.offset(
            zoomingTo = crop.minScale,
            from = 0.6f,
            offset = Offset(150f, 150f),
            anchor = Offset.Zero,
        )

        offset.shouldBeApproximately(Offset.Zero)
    }

    @Test
    fun `given a source matching the output at scale one when the crop rect is computed then it is the identity`() {
        val crop = AvatarCrop(imageSize = Size(512f, 512f), windowSide = 512f)

        val rect = crop.cropRect(scale = 1f, offset = Offset.Zero)

        rect.shouldBeApproximately(Rect(0f, 0f, 512f, 512f))
    }

    @Test
    fun `given a pan and zoom when the crop rect is computed then it maps the visible square to source pixels`() {
        val crop = AvatarCrop(imageSize = Size(2000f, 1000f), windowSide = 300f)

        val rect = crop.cropRect(scale = 0.5f, offset = Offset(25f, -35f))

        rect.shouldBeApproximately(Rect(650f, 270f, 1250f, 870f))
    }
}

private const val EPSILON = 1e-4f

private fun Offset.shouldBeApproximately(expected: Offset) {
    x shouldBe (expected.x plusOrMinus EPSILON)
    y shouldBe (expected.y plusOrMinus EPSILON)
}

private fun Rect.shouldBeApproximately(expected: Rect) {
    left shouldBe (expected.left plusOrMinus EPSILON)
    top shouldBe (expected.top plusOrMinus EPSILON)
    right shouldBe (expected.right plusOrMinus EPSILON)
    bottom shouldBe (expected.bottom plusOrMinus EPSILON)
}
