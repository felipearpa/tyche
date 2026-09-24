package com.felipearpa.tyche.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test

/**
 * The window changes size while the drawer is between endpoints. The host is narrowed or widened
 * in place, as a resizable window would be; a rotation on a phone recreates the activity instead.
 */
class PushDrawerResizeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** The host's width, or `null` to fill the window. */
    private var hostWidth by mutableStateOf<Dp?>(null)

    @Test
    fun given_a_drag_held_midway_when_the_window_narrows_then_the_reveal_keeps_its_progress_and_follows_the_new_width() {
        val harness = setUpDrawer(width = null)
        val wide = drawerWidthPx(windowWidth = null)
        val narrow = drawerWidthPx(windowWidth = NarrowWidth)

        composeTestRule.onRoot().performTouchInput {
            down(Offset(width * 0.1f, height * 0.5f))
            repeat(10) { moveBy(Offset(wide * 0.05f, 0f), delayMillis = 32) }
        }
        composeTestRule.waitForIdle()
        val heldWide = harness.reveal.progress
        val stripWide = pushedScreenLeft()
        hostWidth = NarrowWidth
        composeTestRule.waitForIdle()
        val heldNarrow = harness.reveal.progress
        val stripNarrow = pushedScreenLeft()
        composeTestRule.onRoot().performTouchInput { moveBy(Offset(narrow * 0.2f, 0f), delayMillis = 32) }
        composeTestRule.waitForIdle()
        val moved = harness.reveal.progress
        composeTestRule.onRoot().performTouchInput {
            advanceEventTime(200)
            up()
        }
        composeTestRule.waitForIdle()

        Log.i(LogTag, "held wide=$heldWide narrow=$heldNarrow moved=$moved strip $stripWide -> $stripNarrow")
        heldWide shouldBe (0.48f plusOrMinus 0.05f)
        stripWide shouldBe (wide * heldWide plusOrMinus 2f)
        // The resize keeps the progress, and the pushed screen moves to the new drawer width at
        // once.
        heldNarrow shouldBe heldWide
        stripNarrow shouldBe (narrow * heldWide plusOrMinus 2f)
        // The finger then moves the reveal by the new width.
        moved shouldBe (heldNarrow + 0.2f plusOrMinus 0.01f)
        harness.shouldRest(isOpen = true)
        // Settled open, the dismissal surface is exposed and lines up with the pushed screen.
        dismissalLeft() shouldBe (narrow plusOrMinus 1f)
        pushedScreenLeft() shouldBe (narrow plusOrMinus 1f)
    }

    @Test
    fun given_an_opening_when_the_window_widens_midway_then_it_settles_open_at_the_new_width_and_its_strip_dismisses() {
        val harness = setUpDrawer(width = NarrowWidth)
        val wide = drawerWidthPx(windowWidth = null)
        val hostRowCenterY = composeTestRule.dpToPx(MarkerSize.value + 36f)
        composeTestRule.mainClock.autoAdvance = false

        harness.isOpen = true
        val opening = composeTestRule.frames(count = 4) { harness.reveal.progress }.last()
        hostWidth = null
        val (resizedProgress, resizedStrip) =
            composeTestRule.frames(count = 1) { harness.reveal.progress to pushedScreenLeft() }.single()
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        val openStrip = dismissalLeft()
        val stripCenterX = (wide + windowWidthPx()) / 2f
        composeTestRule.onRoot().performTouchInput { click(Offset(stripCenterX, hostRowCenterY)) }
        composeTestRule.waitForIdle()

        Log.i(LogTag, "opening=$opening resized=$resizedProgress strip=$resizedStrip open=$openStrip")
        opening shouldBeGreaterThan 0.1f
        opening shouldBeLessThan 0.9f
        resizedStrip shouldBe (wide * resizedProgress plusOrMinus 2f)
        openStrip shouldBe (wide plusOrMinus 1f)
        // A tap on the strip beside the wider drawer dismisses it and reaches nothing beneath.
        harness.shouldRest(isOpen = false)
        harness.reportedChanges shouldContainExactly listOf(false)
        harness.hostRowClicks shouldBe 0
    }

    private fun setUpDrawer(width: Dp?): DrawerHarness {
        hostWidth = width
        val harness = DrawerHarness()
        composeTestRule.setContent {
            DrawerHarnessContent(
                harness = harness,
                modifier = hostWidth?.let { Modifier.width(it) } ?: Modifier,
            )
        }
        composeTestRule.waitForIdle()
        return harness
    }

    /** The drawer width in pixels for a portrait window [windowWidth] wide, or the whole window. */
    private fun drawerWidthPx(windowWidth: Dp?): Float = with(composeTestRule.density) {
        drawerWidth(windowWidth ?: windowWidthPx().toDp()).toPx()
    }

    /** The activity's width; the root node is only as wide as a narrowed host. */
    private fun windowWidthPx(): Float = composeTestRule.activity.window.decorView.width.toFloat()

    /** Where the dismissal surface starts; it is exposed only once the drawer has settled open. */
    private fun dismissalLeft(): Float =
        composeTestRule.onNodeWithContentDescription("Close menu").fetchSemanticsNode().boundsInRoot.left

    /**
     * Where the pushed screen starts. While the drawer is modal, the pushed screen is the widest
     * node with cleared semantics; the drawer content, cleared too until it settles open, is
     * narrower.
     */
    private fun pushedScreenLeft(): Float =
        composeTestRule.onAllNodes(SemanticsMatcher("clears semantics") { it.config.isClearingSemantics })
            .fetchSemanticsNodes()
            .maxBy { it.size.width }
            .boundsInRoot.left

    private companion object {
        const val LogTag = "PushDrawerResizeTest"
        val NarrowWidth = 360.dp
    }
}
