package com.felipearpa.tyche.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.revenuecat.placeholder.Pulse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Rule
import org.junit.Test

/**
 * The theme follows the system animator duration scale while placeholders are on screen: a
 * zero scale replaces the pulse with the static midpoint fill, and any nonzero scale brings the
 * pulse back. On a device the window recomposer feeds `Settings.Global.ANIMATOR_DURATION_SCALE`
 * into the same [MotionDurationScale] this test drives.
 */
class LoadingPlaceholderPulseMotionTest {
    private val motionDurationScale = MutableMotionDurationScale()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>(
        effectContext = motionDurationScale,
    )

    @Test
    fun given_animations_turned_off_and_on_while_composed_then_the_pulse_becomes_static_and_returns() {
        var pulse: LoadingPlaceholderPulse? = null
        composeTestRule.setContent {
            TycheTheme(darkTheme = false) {
                pulse = LocalLoadingPlaceholderPulse.current
            }
        }
        composeTestRule.waitForIdle()
        pulse?.highlight.shouldBeInstanceOf<Pulse>()
        pulse?.color shouldBe LoadingPlaceholderPulse.light.dim

        motionDurationScale.scaleFactor = 0f
        composeTestRule.waitForIdle()
        pulse?.highlight.shouldBeNull()
        pulse?.color shouldBe LoadingPlaceholderPulse.light.static

        motionDurationScale.scaleFactor = 0.5f
        composeTestRule.waitForIdle()
        pulse?.highlight.shouldBeInstanceOf<Pulse>()
        pulse?.color shouldBe LoadingPlaceholderPulse.light.dim
    }

    @Test
    fun given_dark_appearance_with_animations_off_then_the_static_fill_is_the_dark_midpoint() {
        motionDurationScale.scaleFactor = 0f
        var pulse: LoadingPlaceholderPulse? = null
        composeTestRule.setContent {
            TycheTheme(darkTheme = true) {
                pulse = LocalLoadingPlaceholderPulse.current
            }
        }
        composeTestRule.waitForIdle()

        pulse?.highlight.shouldBeNull()
        pulse?.color shouldBe LoadingPlaceholderPulse.dark.static
    }
}

private class MutableMotionDurationScale : MotionDurationScale {
    override var scaleFactor by mutableFloatStateOf(1f)
}
