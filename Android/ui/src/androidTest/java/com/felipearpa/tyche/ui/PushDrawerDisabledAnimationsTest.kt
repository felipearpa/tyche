package com.felipearpa.tyche.ui

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

/** System animations turned off, as with a zero animator duration scale. */
class PushDrawerDisabledAnimationsTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>(
        effectContext = object : MotionDurationScale {
            override val scaleFactor = 0f
        },
    )

    @After
    fun restoreTouchMode() {
        // The keyboard test leaves touch mode through a real key press; later tests expect it.
        runCatching { InstrumentationRegistry.getInstrumentation().setInTouchMode(true) }
    }

    @Test
    fun given_disabled_animations_when_the_host_opens_and_closes_the_drawer_then_each_endpoint_is_reached_immediately() {
        val harness = setUpDrawer()
        composeTestRule.mainClock.autoAdvance = false

        harness.isOpen = true
        val opened = composeTestRule.frames(count = 3) { harness.reveal.progress to harness.reveal.phase }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.onNodeWithContentDescription("Close menu").performClick()
        composeTestRule.mainClock.autoAdvance = false
        val closed = composeTestRule.frames(count = 2) { harness.reveal.progress to harness.reveal.phase }
        composeTestRule.mainClock.autoAdvance = true
        // Nothing keeps intercepting input once closed: the host row takes the very next tap.
        composeTestRule.onNodeWithTag(HarnessTags.HostRow).performClick()

        // One frame lets the request reach the drawer; the next one ends the transition.
        opened.indexOfFirst { it == (1f to DrawerPhase.Open) } shouldBeLessThanOrEqual 1
        opened.last() shouldBe (1f to DrawerPhase.Open)
        closed.last() shouldBe (0f to DrawerPhase.Closed)
        harness.hostRowClicks shouldBe 1
        harness.shouldRest(isOpen = false)
    }

    @Test
    fun given_disabled_animations_when_a_drag_is_released_then_it_settles_on_the_next_frame() {
        val harness = setUpDrawer()
        val drawerWidth = composeTestRule.drawerWidthPx()

        composeTestRule.onRoot().performTouchInput {
            down(Offset(width * 0.1f, height * 0.5f))
            repeat(12) { moveBy(Offset(drawerWidth * 0.05f, 0f), delayMillis = 32) }
            advanceEventTime(200)
        }
        val held = harness.reveal.progress
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onRoot().performTouchInput { up() }
        val settled = composeTestRule.frames(count = 2) { harness.reveal.progress }
        composeTestRule.mainClock.autoAdvance = true

        // The drag itself stays under the finger's control.
        held shouldBe (0.58f plusOrMinus 0.05f)
        settled.last() shouldBe 1f
        harness.shouldRest(isOpen = true)
    }

    @Test
    fun given_disabled_animations_when_a_drag_holds_the_drawer_midway_then_the_content_fades_without_the_decorative_scale() {
        val harness = setUpDrawer()
        val drawerWidth = composeTestRule.drawerWidthPx()
        val barColumn = composeTestRule.dpToPx(BarWidth.value / 2f).roundToInt()
        val markerRow = composeTestRule.dpToPx(MarkerSize.value * 0.75f).roundToInt()
        harness.isOpen = true
        composeTestRule.waitForIdle()
        val openPixels = composeTestRule.captureRoot().toPixelMap()
        val barRows = (0 until openPixels.height).filter { openPixels[barColumn, it].isBarBlue() }
        val barRow = barRows[barRows.size / 2]
        harness.isOpen = false
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().performTouchInput {
            down(Offset(width * 0.1f, height * 0.5f))
            repeat(12) { moveBy(Offset(drawerWidth * 0.05f, 0f), delayMillis = 32) }
        }
        composeTestRule.waitForIdle()
        val held = harness.reveal.progress
        val pixels = composeTestRule.captureRoot().toPixelMap()
        composeTestRule.onRoot().performTouchInput { cancel() }
        composeTestRule.waitForIdle()

        val foregroundLeft = pixels.firstX(markerRow) { it.isMarkerRed() }.shouldNotBeNull()
        val barRight = pixels.lastX(barRow, untilX = foregroundLeft) { it.isBarBlue() }.shouldNotBeNull()
        held shouldBe (0.58f plusOrMinus 0.05f)
        // At 0.58 the decorative scale would narrow the 120 dp bar by about 2 dp.
        (barRight + 1).toFloat() shouldBe (composeTestRule.dpToPx(BarWidth.value) plusOrMinus 2f)
        (1f - pixels[2, barRow].red) shouldBe (held plusOrMinus 0.04f)
        harness.shouldRest(isOpen = false)
    }

    @Test
    fun given_disabled_animations_when_back_is_pressed_then_the_drawer_closes_immediately() {
        val harness = setUpDrawer()
        harness.isOpen = true
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        val closed = composeTestRule.frames(count = 2) { harness.reveal.progress to harness.reveal.phase }
        composeTestRule.mainClock.autoAdvance = true

        closed.last() shouldBe (0f to DrawerPhase.Closed)
        harness.reportedChanges shouldContainExactly listOf(false)
        harness.shouldRest(isOpen = false)
    }

    /**
     * With animations off the drawer can go from open to closed between two compositions, so
     * the closed endpoint itself must take keyboard focus off the hidden actions.
     */
    @Test
    fun given_disabled_animations_and_keyboard_focus_tabbed_into_a_touch_opened_drawer_when_escape_closes_it_then_focus_moves_to_the_pushed_screen() {
        val harness = setUpDrawer()
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
        composeTestRule.onNodeWithTag(HarnessTags.Opener).performClick()
        composeTestRule.waitForIdle()
        val focusedInDrawer = composeTestRule.tabIntoDrawer()

        composeTestRule.pressKey(KeyEvent.KEYCODE_ESCAPE)
        val phaseOnceClosed = harness.reveal.phase
        val focusedOnceClosed = composeTestRule.focusedNodeLabels(useUnmergedTree = true)
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)

        assertSoftly {
            focusedInDrawer shouldBeIn DrawerActionLabels
            phaseOnceClosed shouldBe DrawerPhase.Closed
            // Nothing on the pushed screen held keyboard focus before, so it takes its default entry.
            focusedOnceClosed shouldContainExactly listOf(HostRowLabel)
            harness.hostRowClicks shouldBe 1
            harness.drawerRowClicks shouldBe 0
            harness.signOutClicks shouldBe 0
            harness.shouldRest(isOpen = false)
        }
    }

    private fun setUpDrawer(): DrawerHarness {
        val harness = DrawerHarness()
        composeTestRule.setContent { DrawerHarnessContent(harness) }
        composeTestRule.waitForIdle()
        return harness
    }
}
