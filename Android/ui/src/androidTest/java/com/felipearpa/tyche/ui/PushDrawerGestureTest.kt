package com.felipearpa.tyche.ui

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test

class PushDrawerGestureTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun given_a_closed_drawer_when_swiping_toward_the_trailing_edge_from_a_host_row_then_it_opens_and_the_row_does_not_activate() {
        val harness = setUpDrawer()

        composeTestRule.onNodeWithTag(HarnessTags.HostRow).performTouchInput {
            swipe(start = centerLeft + Offset(width * 0.1f, 0f), end = center + Offset(width * 0.3f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = true)
        harness.reportedChanges shouldContainExactly listOf(true)
        harness.hostRowClicks shouldBe 0
    }

    @Test
    fun given_a_closed_drawer_when_a_slow_drag_is_released_before_the_midpoint_then_it_settles_back_closed() {
        val harness = setUpDrawer()
        val drawerWidth = composeTestRule.drawerWidthPx()

        composeTestRule.onRoot().performTouchInput {
            down(Offset(width * 0.2f, height * 0.5f))
            repeat(10) { moveBy(Offset(drawerWidth * 0.035f, 0f), delayMillis = 32) }
            advanceEventTime(200)
            up()
        }
        val released = harness.reveal.progress
        composeTestRule.waitForIdle()

        released shouldBe (0.35f plusOrMinus 0.05f)
        harness.shouldRest(isOpen = false)
        harness.reportedChanges.shouldBeEmpty()
    }

    @Test
    fun given_a_closed_drawer_when_a_slow_drag_is_released_past_the_midpoint_then_it_settles_open() {
        val harness = setUpDrawer()
        val drawerWidth = composeTestRule.drawerWidthPx()

        composeTestRule.onRoot().performTouchInput {
            down(Offset(width * 0.1f, height * 0.5f))
            repeat(13) { moveBy(Offset(drawerWidth * 0.05f, 0f), delayMillis = 32) }
            advanceEventTime(200)
            up()
        }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = true)
        harness.reportedChanges shouldContainExactly listOf(true)
    }

    @Test
    fun given_a_closed_drawer_when_a_short_fast_flick_heads_toward_the_trailing_edge_then_it_opens() {
        val harness = setUpDrawer()

        composeTestRule.onRoot().performTouchInput {
            val start = Offset(width * 0.3f, height * 0.5f)
            swipe(start = start, end = start + Offset(width * 0.15f, 0f), durationMillis = 60)
        }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = true)
    }

    @Test
    fun given_an_open_drawer_when_a_short_fast_flick_heads_toward_the_leading_edge_then_it_closes() {
        val harness = setUpDrawer(initiallyOpen = true)

        composeTestRule.onRoot().performTouchInput {
            val start = Offset(width * 0.6f, height * 0.5f)
            swipe(start = start, end = start - Offset(width * 0.15f, 0f), durationMillis = 60)
        }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = false)
        harness.reportedChanges shouldContainExactly listOf(false)
    }

    @Test
    fun given_an_open_drawer_when_dragging_from_a_drawer_row_then_it_closes_and_the_row_does_not_activate() {
        val harness = setUpDrawer(initiallyOpen = true)

        composeTestRule.onNodeWithTag(HarnessTags.DrawerRow).performTouchInput {
            swipe(start = center, end = center - Offset(width * 0.4f, 0f), durationMillis = 250)
        }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = false)
        harness.drawerRowClicks shouldBe 0
    }

    @Test
    fun given_an_open_drawer_when_dragging_the_pushed_screen_strip_then_it_closes() {
        val harness = setUpDrawer(initiallyOpen = true)

        composeTestRule.onRoot().performTouchInput {
            val start = Offset(width * 0.92f, height * 0.3f)
            swipe(start = start, end = start - Offset(width * 0.6f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = false)
        harness.hostRowClicks shouldBe 0
    }

    @Test
    fun given_drawer_and_host_rows_when_tapped_then_each_activates_once() {
        val harness = setUpDrawer()

        composeTestRule.onNodeWithTag(HarnessTags.HostRow).performClick()
        harness.isOpen = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(HarnessTags.DrawerRow).performClick()
        composeTestRule.waitForIdle()

        harness.hostRowClicks shouldBe 1
        harness.drawerRowClicks shouldBe 1
        harness.shouldRest(isOpen = true)
    }

    @Test
    fun given_a_closed_drawer_when_a_drag_past_the_midpoint_is_cancelled_then_it_returns_closed_without_activating_the_row() {
        val harness = setUpDrawer()
        val drawerWidth = composeTestRule.drawerWidthPx()

        composeTestRule.onNodeWithTag(HarnessTags.HostRow).performTouchInput {
            down(centerLeft + Offset(width * 0.05f, 0f))
            repeat(10) { moveBy(Offset(drawerWidth * 0.07f, 0f), delayMillis = 32) }
        }
        val held = harness.reveal.progress
        composeTestRule.onRoot().performTouchInput { cancel() }
        composeTestRule.waitForIdle()

        held shouldBeGreaterThan 0.5f
        harness.shouldRest(isOpen = false)
        harness.reportedChanges.shouldBeEmpty()
        harness.hostRowClicks shouldBe 0
    }

    @Test
    fun given_an_open_drawer_when_a_drag_past_the_midpoint_is_cancelled_then_it_returns_open() {
        val harness = setUpDrawer(initiallyOpen = true)
        val drawerWidth = composeTestRule.drawerWidthPx()

        composeTestRule.onNodeWithTag(HarnessTags.DrawerRow).performTouchInput {
            down(centerRight - Offset(width * 0.05f, 0f))
            repeat(8) { moveBy(Offset(-drawerWidth * 0.07f, 0f), delayMillis = 32) }
        }
        val held = harness.reveal.progress
        composeTestRule.onRoot().performTouchInput { cancel() }
        composeTestRule.waitForIdle()

        held shouldBeLessThan 0.5f
        harness.shouldRest(isOpen = true)
        harness.drawerRowClicks shouldBe 0
    }

    @Test
    fun given_an_opening_transition_when_a_drag_grabs_it_then_it_continues_from_the_presented_progress() {
        val harness = setUpDrawer()
        composeTestRule.mainClock.autoAdvance = false
        harness.isOpen = true
        val presented = composeTestRule.frames(count = 5) { harness.reveal.progress }.last()

        composeTestRule.onRoot().performTouchInput {
            down(Offset(width * 0.95f, height * 0.5f))
            moveBy(Offset(-viewConfiguration.touchSlop * 2f, 0f))
        }
        composeTestRule.waitForIdle()
        val grabbed = harness.reveal.progress
        val held = composeTestRule.frames(count = 10) { harness.reveal.progress }
        val drawerWidth = composeTestRule.drawerWidthPx()
        composeTestRule.onRoot().performTouchInput {
            moveBy(Offset(-drawerWidth * 0.25f, 0f))
        }
        composeTestRule.waitForIdle()
        val dragged = harness.reveal.progress

        Log.i(LogTag, "takeover presented=$presented grabbed=$grabbed dragged=$dragged")
        presented shouldBeGreaterThan 0.1f
        presented shouldBeLessThan 0.9f
        harness.reveal.phase shouldBe DrawerPhase.Dragging
        // No jump: the drag starts from what was on screen (injecting the touch can let one more
        // frame of the opening run first), and the transition stops following.
        grabbed shouldBe (presented plusOrMinus 0.15f)
        held.forEach { it shouldBe grabbed }
        dragged shouldBe (grabbed - 0.25f plusOrMinus 0.01f)
        composeTestRule.onRoot().performTouchInput { cancel() }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        // The opening never came to rest, so a cancelled grab returns closed, and says so.
        harness.shouldRest(isOpen = false)
        harness.reportedChanges shouldContainExactly listOf(false)
    }

    @Test
    fun given_a_host_list_when_dragging_it_vertically_then_it_scrolls_and_the_drawer_stays_closed() {
        val harness = setUpDrawer()

        composeTestRule.onNodeWithTag(HarnessTags.List).performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()

        harness.listState.firstVisibleItemIndex shouldBeGreaterThan 0
        harness.shouldRest(isOpen = false)
        harness.itemClicks shouldBe 0
    }

    @Test
    fun given_a_nearly_vertical_drag_on_a_host_row_when_released_then_the_drawer_stays_closed() {
        val harness = setUpDrawer()

        composeTestRule.onNodeWithTag(HarnessTags.List).performTouchInput {
            swipe(start = center, end = center + Offset(width * 0.1f, -height * 0.4f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = false)
        harness.listState.firstVisibleItemIndex shouldBeGreaterThan 0
    }

    @Test
    fun given_a_horizontal_child_scroller_when_dragging_it_either_way_then_it_keeps_its_drag() {
        val harness = setUpDrawer()

        composeTestRule.onNodeWithTag(HarnessTags.Chips).performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        val scrolled = harness.chipsState.firstVisibleItemIndex
        composeTestRule.onNodeWithTag(HarnessTags.Chips).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        scrolled shouldBeGreaterThan 0
        harness.chipsState.firstVisibleItemIndex shouldBeLessThan scrolled
        harness.shouldRest(isOpen = false)
    }

    @Test
    fun given_a_horizontal_child_scroller_at_its_start_when_swiping_toward_the_trailing_edge_then_it_keeps_the_drag_and_the_drawer_stays_closed() {
        val harness = setUpDrawer()

        composeTestRule.onNodeWithTag(HarnessTags.Chips).performTouchInput {
            swipe(start = centerLeft + Offset(width * 0.1f, 0f), end = center + Offset(width * 0.3f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()

        // The scroller cannot move further back, but it claims the drag (its overscroll) before the
        // drawer sees it.
        harness.chipsState.firstVisibleItemIndex shouldBe 0
        harness.shouldRest(isOpen = false)
        harness.reportedChanges.shouldBeEmpty()
    }

    @Test
    fun given_a_closed_drawer_when_dragging_a_synthetic_horizontal_control_either_way_then_the_control_keeps_its_drag() {
        val harness = setUpDrawer()
        val rootWidth = composeTestRule.rootWidth()

        composeTestRule.onNodeWithTag(HarnessTags.HostControl).performTouchInput {
            swipe(start = centerLeft + Offset(width * 0.1f, 0f), end = center + Offset(width * 0.3f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()
        val trailing = harness.hostControlDrag
        composeTestRule.onNodeWithTag(HarnessTags.HostControl).performTouchInput {
            swipe(start = center + Offset(width * 0.3f, 0f), end = centerLeft + Offset(width * 0.1f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()
        val leading = harness.hostControlDrag - trailing

        Log.i(LogTag, "host control trailing=$trailing leading=$leading")
        // Each swipe covers 0.7 of the width; the control receives it all past its touch slop.
        trailing shouldBeGreaterThan rootWidth * 0.6f
        leading shouldBeLessThan -rootWidth * 0.6f
        harness.shouldRest(isOpen = false)
        harness.reportedChanges.shouldBeEmpty()
    }

    @Test
    fun given_an_open_drawer_when_dragging_a_synthetic_horizontal_control_in_it_then_the_control_keeps_its_drag_and_the_drawer_stays_open() {
        val harness = setUpDrawer(initiallyOpen = true)

        composeTestRule.onNodeWithTag(HarnessTags.DrawerControl).performTouchInput {
            swipe(start = center + Offset(width * 0.3f, 0f), end = centerLeft + Offset(width * 0.1f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()

        Log.i(LogTag, "drawer control leading=${harness.drawerControlDrag}")
        harness.drawerControlDrag shouldBeLessThan -composeTestRule.drawerWidthPx() * 0.5f
        harness.shouldRest(isOpen = true)
        harness.reportedChanges.shouldBeEmpty()
    }

    @Test
    fun given_a_closing_transition_when_a_drag_grabs_it_and_is_cancelled_then_it_returns_open() {
        val harness = setUpDrawer(initiallyOpen = true)
        composeTestRule.mainClock.autoAdvance = false
        harness.isOpen = false
        val presented = composeTestRule.frames(count = 5) { harness.reveal.progress }.last()

        composeTestRule.onRoot().performTouchInput {
            down(Offset(width * 0.95f, height * 0.5f))
            moveBy(Offset(viewConfiguration.touchSlop * 2f, 0f))
        }
        composeTestRule.waitForIdle()
        val grabbed = harness.reveal.progress
        val held = composeTestRule.frames(count = 10) { harness.reveal.progress }
        composeTestRule.onRoot().performTouchInput { cancel() }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        Log.i(LogTag, "closing takeover presented=$presented grabbed=$grabbed")
        presented shouldBeGreaterThan 0.1f
        presented shouldBeLessThan 0.9f
        grabbed shouldBe (presented plusOrMinus 0.15f)
        held.forEach { it shouldBe grabbed }
        // The drawer last rested open, so the cancelled grab returns there and asks the host to
        // follow, although the host had asked it to close.
        harness.shouldRest(isOpen = true)
        harness.reportedChanges shouldContainExactly listOf(true)
        harness.hostRowClicks shouldBe 0
    }

    @Test
    fun given_the_tab_row_when_a_tab_is_tapped_then_it_selects_and_the_drawer_stays_closed() {
        val harness = setUpDrawer()

        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.waitForIdle()

        harness.selectedTab shouldBe 2
        harness.shouldRest(isOpen = false)
    }

    @Test
    fun given_the_tab_row_when_swiping_toward_the_trailing_edge_across_it_then_the_drawer_opens_and_no_tab_is_selected() {
        val harness = setUpDrawer()

        composeTestRule.onNodeWithTag(HarnessTags.Tabs).performTouchInput {
            swipe(start = centerLeft + Offset(width * 0.1f, 0f), end = center + Offset(width * 0.3f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = true)
        harness.selectedTab shouldBe 0
    }

    @Test
    fun given_a_closed_drawer_when_swiping_toward_the_leading_edge_on_a_host_row_then_nothing_opens_or_activates() {
        val harness = setUpDrawer()

        composeTestRule.onNodeWithTag(HarnessTags.HostRow).performTouchInput {
            swipe(start = center, end = center - Offset(width * 0.3f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = false)
        harness.hostRowClicks shouldBe 0
        harness.reportedChanges.shouldBeEmpty()
    }

    @Test
    fun given_a_right_to_left_layout_when_swiping_leftward_then_it_opens_and_rightward_closes_it() {
        val harness = setUpDrawer(layoutDirection = LayoutDirection.Rtl)

        composeTestRule.onRoot().performTouchInput {
            val start = Offset(width * 0.1f, height * 0.5f)
            swipe(start = start, end = start + Offset(width * 0.5f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()
        val afterRightwardWhileClosed = harness.reveal.progress
        composeTestRule.onRoot().performTouchInput {
            val start = Offset(width * 0.8f, height * 0.5f)
            swipe(start = start, end = start - Offset(width * 0.6f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()
        val afterLeftward = harness.reveal.progress
        composeTestRule.onRoot().performTouchInput {
            val start = Offset(width * 0.3f, height * 0.5f)
            swipe(start = start, end = start + Offset(width * 0.6f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()

        afterRightwardWhileClosed shouldBe 0f
        afterLeftward shouldBe 1f
        harness.shouldRest(isOpen = false)
        harness.reportedChanges shouldContainExactly listOf(true, false)
    }

    @Test
    fun given_a_system_gesture_edge_when_a_drag_starts_there_then_the_drawer_leaves_it_to_the_system() {
        val harness = setUpDrawer(systemGestureInsets = WindowInsets(left = 32.dp, right = 32.dp))
        val edge = composeTestRule.dpToPx(32f)

        composeTestRule.onRoot().performTouchInput {
            val start = Offset(edge * 0.5f, height * 0.6f)
            swipe(start = start, end = start + Offset(width * 0.6f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()
        val fromEdge = harness.reveal.progress
        composeTestRule.onRoot().performTouchInput {
            val start = Offset(edge * 2f, height * 0.6f)
            swipe(start = start, end = start + Offset(width * 0.6f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()

        fromEdge shouldBe 0f
        harness.shouldRest(isOpen = true)
    }

    @Test
    fun given_a_host_that_keeps_the_drawer_closed_when_a_swipe_opens_it_then_it_settles_back_closed() {
        val harness = setUpDrawer()
        harness.acceptsChanges = false

        composeTestRule.onRoot().performTouchInput {
            val start = Offset(width * 0.1f, height * 0.5f)
            swipe(start = start, end = start + Offset(width * 0.7f, 0f), durationMillis = 300)
        }
        composeTestRule.waitForIdle()

        harness.reportedChanges shouldContainExactly listOf(true)
        harness.shouldRest(isOpen = false)
    }

    private fun setUpDrawer(
        initiallyOpen: Boolean = false,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        systemGestureInsets: WindowInsets = WindowInsets(0.dp),
    ): DrawerHarness {
        val harness = DrawerHarness(initiallyOpen)
        composeTestRule.setContent {
            DrawerHarnessContent(
                harness = harness,
                layoutDirection = layoutDirection,
                systemGestureInsets = systemGestureInsets,
            )
        }
        composeTestRule.waitForIdle()
        return harness
    }

    private companion object {
        const val LogTag = "PushDrawerGestureTest"
    }
}

/** The drawer rests at an endpoint that matches the host's state: never a partial state. */
internal fun DrawerHarness.shouldRest(isOpen: Boolean) {
    this.isOpen shouldBe isOpen
    reveal.progress shouldBe if (isOpen) 1f else 0f
    reveal.phase shouldBe if (isOpen) DrawerPhase.Open else DrawerPhase.Closed
}
