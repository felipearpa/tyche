package com.felipearpa.tyche.ui.loading

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.ui.R
import com.felipearpa.tyche.ui.captureRoot
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The ball's angle after a number of frames, under a given animator duration scale, compared with
 * the same ball drawn upright and at the angle a two-second turn reaches at that scale.
 */
abstract class BallSpinnerMotionTest(private val scale: Float) {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>(
        effectContext = object : MotionDurationScale {
            override val scaleFactor = scale
        },
    )

    /**
     * Draws the spinner beside two still references and runs [Frames] frames after the one that
     * starts the rotation. Returns how much the spinner differs from the upright reference and from
     * the [expectedDegrees] reference.
     */
    protected fun spinAndCompare(expectedDegrees: Float): Pair<Double, Double> {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            TycheTheme {
                Surface {
                    Row {
                        Box(Modifier.testTag(SpinnerTag)) { BallSpinner() }
                        ReferenceBall(degrees = 0f, modifier = Modifier.testTag(UprightTag))
                        ReferenceBall(degrees = expectedDegrees, modifier = Modifier.testTag(ExpectedTag))
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        // The first frame starts the rotation at 0°.
        composeTestRule.mainClock.advanceTimeByFrame()
        repeat(Frames) { composeTestRule.mainClock.advanceTimeByFrame() }
        composeTestRule.waitForIdle()

        val root = composeTestRule.captureRoot().toPixelMap()
        val spinner = composeTestRule.crop(root, SpinnerTag)
        return spinner.differenceFrom(composeTestRule.crop(root, UprightTag)) to
            spinner.differenceFrom(composeTestRule.crop(root, ExpectedTag))
    }

    protected fun expectedDegreesAfterFrames(): Float =
        (Frames * FrameMillis / scale / TurnMillis * 360f) % 360f

    companion object {
        /** 25 frames of 16 ms: 400 ms, a fifth of a turn at normal speed. */
        const val Frames = 25
        private const val FrameMillis = 16f
        private const val TurnMillis = 2000f
    }
}

class BallSpinnerNormalMotionTest : BallSpinnerMotionTest(scale = 1f) {
    @Test
    fun given_normal_animation_speed_when_400_ms_pass_then_the_ball_has_turned_72_degrees() {
        expectedDegreesAfterFrames() shouldBe (72f plusOrMinus 0.01f)

        val (fromUpright, fromExpected) = spinAndCompare(expectedDegrees = 72f)

        assertSoftly {
            withClue("matches the 72° reference") { fromExpected shouldBeLessThan SameImage }
            withClue("has left the upright position") { fromUpright shouldBeGreaterThan DifferentImage }
        }
    }
}

class BallSpinnerFastMotionTest : BallSpinnerMotionTest(scale = 0.5f) {
    @Test
    fun given_half_animation_duration_when_400_ms_pass_then_the_ball_has_turned_twice_as_far() {
        expectedDegreesAfterFrames() shouldBe (144f plusOrMinus 0.01f)

        val (fromUpright, fromExpected) = spinAndCompare(expectedDegrees = 144f)

        assertSoftly {
            withClue("matches the 144° reference") { fromExpected shouldBeLessThan SameImage }
            withClue("has left the upright position") { fromUpright shouldBeGreaterThan DifferentImage }
        }
    }
}

class BallSpinnerDisabledMotionTest : BallSpinnerMotionTest(scale = 0f) {
    @Test
    fun given_disabled_animations_when_time_passes_then_the_ball_stays_upright() {
        val (fromUpright, fromTurned) = spinAndCompare(expectedDegrees = 72f)

        assertSoftly {
            withClue("matches the upright reference") { fromUpright shouldBeLessThan SameImage }
            withClue("differs from a turned ball") { fromTurned shouldBeGreaterThan DifferentImage }
        }
    }
}

@Composable
private fun ReferenceBall(degrees: Float, modifier: Modifier = Modifier) {
    Box(modifier) {
        Icon(
            painter = painterResource(id = R.drawable.tyche_logo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(64.dp)
                .rotate(degrees),
        )
    }
}

private fun ComposeTestRule.crop(root: PixelMap, tag: String): List<Int> {
    val node = onNodeWithTag(tag).fetchSemanticsNode()
    val left = node.positionInRoot.x.roundToInt()
    val top = node.positionInRoot.y.roundToInt()
    return buildList {
        for (y in top until top + node.size.height) {
            for (x in left until left + node.size.width) {
                val color = root[x, y]
                add(((color.red + color.green + color.blue) * 255f / 3f).roundToInt())
            }
        }
    }
}

/** The share of pixels whose brightness differs by more than a tenth. */
private fun List<Int>.differenceFrom(other: List<Int>): Double =
    zip(other).count { (a, b) -> abs(a - b) > 25 }.toDouble() / size

/** Rendering the same picture twice differs at most at a few antialiased edge pixels. */
private const val SameImage = 0.01

/** A fifth of a turn moves well over this share of the ball's pixels. */
private const val DifferentImage = 0.05

private const val SpinnerTag = "spinner"
private const val UprightTag = "upright"
private const val ExpectedTag = "expected"
