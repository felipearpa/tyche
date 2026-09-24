package com.felipearpa.tyche.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.util.lerp
import io.kotest.matchers.collections.shouldBeMonotonicallyDecreasing
import io.kotest.matchers.collections.shouldBeMonotonicallyIncreasing
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

class PushDrawerRevealTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun given_a_closed_drawer_when_the_host_opens_and_closes_it_then_each_request_settles_at_its_endpoint() {
        val harness = DrawerHarness()
        composeTestRule.setContent { DrawerHarnessContent(harness) }

        harness.isOpen = true
        composeTestRule.waitForIdle()
        val opened = harness.reveal.progress to harness.reveal.phase
        harness.isOpen = false
        composeTestRule.waitForIdle()

        opened shouldBe (1f to DrawerPhase.Open)
        harness.reveal.progress shouldBe 0f
        harness.reveal.phase shouldBe DrawerPhase.Closed
        harness.reportedChanges.shouldHaveSize(0)
    }

    @Test
    fun given_an_opening_drawer_when_the_host_closes_it_midway_then_it_reverses_from_the_displayed_progress() {
        val harness = DrawerHarness()
        composeTestRule.setContent { DrawerHarnessContent(harness) }
        composeTestRule.mainClock.autoAdvance = false

        harness.isOpen = true
        val opening = composeTestRule.frames(count = 6) { harness.reveal.progress }
        harness.isOpen = false
        val reversing = composeTestRule.frames(count = 60) { harness.reveal.progress }

        Log.i(LogTag, "reversal opening=${opening.rounded()} reversing=${reversing.take(12).rounded()}")
        val displayed = opening.last()
        displayed shouldBeGreaterThan 0.1f
        displayed shouldBeLessThan 0.9f
        // No frame jumps: the request reaches the drawer a frame later, momentum carries it a
        // little further, and the spring turns it around from there.
        (listOf(displayed) + reversing).largestStep() shouldBeLessThan opening.largestStep() + StepTolerance
        reversing.indexOf(reversing.max()) shouldBeLessThanOrEqual 3
        reversing.max() shouldBeLessThan 1f
        reversing.last() shouldBe 0f
        harness.reveal.phase shouldBe DrawerPhase.Closed
    }

    @Test
    fun given_a_closing_drawer_when_the_host_reopens_it_midway_then_it_reverses_and_settles_open() {
        val harness = DrawerHarness(initiallyOpen = true)
        composeTestRule.setContent { DrawerHarnessContent(harness) }
        composeTestRule.mainClock.autoAdvance = false

        harness.isOpen = false
        val closing = composeTestRule.frames(count = 6) { harness.reveal.progress }
        harness.isOpen = true
        val reopening = composeTestRule.frames(count = 60) { harness.reveal.progress }

        Log.i(LogTag, "reopen closing=${closing.rounded()} reopening=${reopening.take(12).rounded()}")
        val displayed = closing.last()
        displayed shouldBeGreaterThan 0.1f
        displayed shouldBeLessThan 0.9f
        (listOf(displayed) + reopening).largestStep() shouldBeLessThan closing.largestStep() + StepTolerance
        reopening.indexOf(reopening.min()) shouldBeLessThanOrEqual 3
        reopening.min() shouldBeGreaterThan 0f
        reopening.last() shouldBe 1f
        harness.reveal.phase shouldBe DrawerPhase.Open
    }

    @Test
    fun given_a_drawer_when_it_opens_and_closes_frame_by_frame_then_every_visual_property_follows_one_progress() {
        val harness = DrawerHarness()
        composeTestRule.setContent { DrawerHarnessContent(harness) }
        val geometry = measureGeometry(harness)
        val labelLayoutsBefore = harness.drawerLabelLayouts.toList()
        composeTestRule.mainClock.autoAdvance = false

        harness.isOpen = true
        val opening = composeTestRule.frames(count = 40) { captureFrame(harness, geometry) }
        harness.isOpen = false
        val closing = composeTestRule.frames(count = 40) { captureFrame(harness, geometry) }

        val frames = opening + closing
        frames.forEach { Log.i(LogTag, "frame $it") }
        opening.map { it.progress }.shouldBeMonotonicallyIncreasing()
        closing.map { it.progress }.shouldBeMonotonicallyDecreasing()
        opening.last().progress shouldBe 1f
        closing.last().progress shouldBe 0f

        val midway = frames.filter { it.progress in 0.05f..0.95f }
        midway.size shouldBeGreaterThanOrEqual 10
        midway.forEach { frame ->
            val progress = frame.progress
            // The pushed screen moves by the drawer width times the progress...
            val foregroundLeft = frame.foregroundLeft.shouldNotBeNull()
            abs(foregroundLeft - geometry.drawerWidth * progress) shouldBeLessThan 4f
            // ...and dims with the same progress.
            frame.markerRed shouldBe ((1f - LightScrimAlpha * progress) plusOrMinus 0.02f)
            // The drawer surface behind the content stays opaque and unchanged.
            frame.surface shouldBe Color.White
            // The content group fades in with the progress...
            frame.barAlpha shouldBe (progress plusOrMinus 0.04f)
            // ...and, where the whole bar is uncovered, grows from the closed scale.
            if (geometry.drawerWidth * progress > geometry.barWidth + geometry.markerHalf) {
                val width = frame.barWidth.shouldNotBeNull()
                width shouldBe (geometry.barWidth * lerp(ClosedContentScale, 1f, progress) plusOrMinus 3f)
            }
        }
        // The content is still drawn late in the exit: it fades out rather than disappearing.
        closing.filter { it.progress in 0.12f..0.3f }.forEach { it.barAlpha shouldBeGreaterThan 0.08f }
        // Scaling is a render transform: the wrapping label was never laid out again.
        harness.drawerLabelLayouts shouldContainExactly labelLayoutsBefore
        labelLayoutsBefore.last() shouldBeGreaterThanOrEqual 2
    }

    @Test
    fun given_a_dark_theme_when_the_drawer_is_open_then_the_pushed_screen_dims_with_the_dark_scrim() {
        val harness = DrawerHarness(initiallyOpen = true)
        composeTestRule.setContent { DrawerHarnessContent(harness, darkTheme = true) }
        composeTestRule.waitForIdle()
        val drawerWidth = composeTestRule.drawerWidthPx().roundToInt()
        val markerRow = composeTestRule.dpToPx(MarkerSize.value * 0.75f).roundToInt()
        val markerHalf = composeTestRule.dpToPx(MarkerSize.value / 2f).roundToInt()

        val pixels = composeTestRule.captureRoot().toPixelMap()

        pixels[drawerWidth + markerHalf, markerRow].red shouldBe ((1f - DarkScrimAlpha) plusOrMinus 0.02f)
        pixels[composeTestRule.dpToPx(8f).roundToInt(), pixels.height / 4 * 3] shouldBe Color(0xFF121212)
    }

    /** Opens the drawer once to find where the bar sits, then closes it again. */
    private fun measureGeometry(harness: DrawerHarness): FrameGeometry {
        harness.isOpen = true
        composeTestRule.waitForIdle()
        val pixels = composeTestRule.captureRoot().toPixelMap()
        val barColumn = composeTestRule.dpToPx(BarWidth.value / 2f).roundToInt()
        val barRows = (0 until pixels.height).filter { pixels[barColumn, it].isBarBlue() }
        harness.isOpen = false
        composeTestRule.waitForIdle()

        return FrameGeometry(
            drawerWidth = composeTestRule.drawerWidthPx(),
            markerRow = composeTestRule.dpToPx(MarkerSize.value * 0.75f).roundToInt(),
            markerHalf = composeTestRule.dpToPx(MarkerSize.value / 2f).roundToInt(),
            barRow = barRows[barRows.size / 2],
            barWidth = composeTestRule.dpToPx(BarWidth.value),
            surfaceProbeX = composeTestRule.dpToPx(8f).roundToInt(),
            surfaceProbeY = pixels.height / 4 * 3,
        )
    }

    private fun captureFrame(harness: DrawerHarness, geometry: FrameGeometry): RevealFrame {
        val pixels = composeTestRule.captureRoot().toPixelMap()
        val foregroundLeft = pixels.firstX(geometry.markerRow) { it.isMarkerRed() }
        val barRight = foregroundLeft?.let { left ->
            pixels.lastX(geometry.barRow, untilX = left) { it.isBarBlue() }
        }
        return RevealFrame(
            progress = harness.reveal.progress,
            foregroundLeft = foregroundLeft?.toFloat(),
            markerRed = foregroundLeft?.let {
                pixels[(it + geometry.markerHalf).coerceAtMost(pixels.width - 1), geometry.markerRow].red
            } ?: 0f,
            surface = pixels[geometry.surfaceProbeX, geometry.surfaceProbeY],
            // The bar starts at the leading edge, where the scale is anchored.
            barAlpha = 1f - pixels[2, geometry.barRow].red,
            barWidth = barRight?.let { (it + 1).toFloat() },
        )
    }

    private fun List<Float>.rounded() = map { (it * 1000).roundToInt() / 1000f }

    /** The largest change of progress between consecutive frames. */
    private fun List<Float>.largestStep() = zipWithNext { previous, next -> abs(next - previous) }.max()

    private companion object {
        const val LogTag = "PushDrawerRevealTest"
        const val StepTolerance = 0.02f
    }
}

private class FrameGeometry(
    val drawerWidth: Float,
    val markerRow: Int,
    val markerHalf: Int,
    val barRow: Int,
    val barWidth: Float,
    val surfaceProbeX: Int,
    val surfaceProbeY: Int,
)

private data class RevealFrame(
    val progress: Float,
    val foregroundLeft: Float?,
    val markerRed: Float,
    val surface: Color,
    val barAlpha: Float,
    val barWidth: Float?,
)
