package com.felipearpa.tyche.ui

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test

class PushDrawerDismissalTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun given_an_open_drawer_when_tapping_the_pushed_screen_over_a_control_then_it_closes_and_the_control_does_not_activate() {
        val harness = setUpDrawer(initiallyOpen = true)
        val hostRowCenterY = composeTestRule.dpToPx(MarkerSize.value + 36f)

        composeTestRule.onRoot().performTouchInput {
            click(Offset(width * 0.95f, hostRowCenterY))
        }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = false)
        harness.reportedChanges shouldContainExactly listOf(false)
        harness.hostRowClicks shouldBe 0
        harness.openerClicks shouldBe 0
    }

    @Test
    fun given_a_closing_drawer_when_tapping_where_the_pushed_screen_controls_are_then_none_activates() {
        val harness = setUpDrawer(initiallyOpen = true)
        val drawerWidth = composeTestRule.drawerWidthPx()
        val hostRowCenterY = composeTestRule.dpToPx(MarkerSize.value + 36f)
        val openerCenter = Offset(composeTestRule.dpToPx(MarkerSize.value + 24f), composeTestRule.dpToPx(24f))
        composeTestRule.mainClock.autoAdvance = false

        harness.isOpen = false
        val closing = composeTestRule.frames(count = 4) { harness.reveal.progress }.last()
        val offset = drawerWidth * closing
        composeTestRule.onRoot().performTouchInput {
            // Where the host row and the opener are drawn right now, pushed by the reveal.
            click(Offset(offset + width * 0.3f, hostRowCenterY))
            click(openerCenter + Offset(offset, 0f))
        }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(HarnessTags.HostRow).performClick()

        closing shouldBeGreaterThan 0.1f
        closing shouldBeLessThan 0.95f
        harness.openerClicks shouldBe 0
        // Only the tap made after the drawer closed reached the row.
        harness.hostRowClicks shouldBe 1
        harness.shouldRest(isOpen = false)
    }

    @Test
    fun given_an_open_drawer_when_its_accessibility_dismissal_action_is_performed_then_it_closes() {
        val harness = setUpDrawer(initiallyOpen = true)

        // The click action TalkBack performs, not a touch.
        composeTestRule.onNodeWithContentDescription("Close menu").performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = false)
        harness.hostRowClicks shouldBe 0
    }

    @Test
    fun given_an_open_drawer_on_a_pushed_route_when_back_is_pressed_then_the_drawer_closes_before_the_route_pops() {
        val harness = DrawerHarness()
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = PreviousRoute) {
                composable(PreviousRoute) { Text("Previous route") }
                composable(HostRoute) { DrawerHarnessContent(harness) }
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(HostRoute) }
        composeTestRule.waitForIdle()
        harness.isOpen = true
        composeTestRule.waitForIdle()

        Espresso.pressBack()
        composeTestRule.waitForIdle()
        val routeAfterFirstBack = navController.currentDestination?.route
        val closedAfterFirstBack = harness.reveal.phase
        Espresso.pressBack()
        composeTestRule.waitForIdle()

        routeAfterFirstBack shouldBe HostRoute
        closedAfterFirstBack shouldBe DrawerPhase.Closed
        harness.isOpen shouldBe false
        navController.currentDestination?.route shouldBe PreviousRoute
    }

    @Test
    fun given_an_opening_drawer_on_a_pushed_route_when_back_is_pressed_then_it_turns_around_and_closes_before_the_route_pops() {
        val harness = DrawerHarness()
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = PreviousRoute) {
                composable(PreviousRoute) { Text("Previous route") }
                composable(HostRoute) { DrawerHarnessContent(harness) }
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(HostRoute) }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        harness.isOpen = true
        val opening = composeTestRule.frames(count = 4) { harness.reveal.progress }.last()
        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        val afterBack = composeTestRule.frames(count = 60) { harness.reveal.progress }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        opening shouldBeGreaterThan 0.1f
        opening shouldBeLessThan 0.9f
        // Back ends the opening where it is: it never reaches open on the way.
        afterBack.max() shouldBeLessThan opening + 0.15f
        afterBack.last() shouldBe 0f
        harness.shouldRest(isOpen = false)
        harness.reportedChanges shouldContainExactly listOf(false)
        navController.currentDestination?.route shouldBe HostRoute
    }

    @Test
    fun given_an_open_drawer_when_a_predictive_back_gesture_progresses_and_is_cancelled_then_it_follows_and_returns_open() {
        val harness = setUpDrawer(initiallyOpen = true)
        val dispatcher = composeTestRule.activity.onBackPressedDispatcher

        composeTestRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(backEvent(progress = 0f))
            dispatcher.dispatchOnBackProgressed(backEvent(progress = 0.4f))
        }
        composeTestRule.waitForIdle()
        val tracked = harness.reveal.progress
        val trackedPhase = harness.reveal.phase
        composeTestRule.runOnUiThread { dispatcher.dispatchOnBackCancelled() }
        composeTestRule.waitForIdle()

        tracked shouldBe (0.6f plusOrMinus 0.01f)
        trackedPhase shouldBe DrawerPhase.Dragging
        harness.shouldRest(isOpen = true)
        harness.reportedChanges.shouldBeEmpty()
    }

    @Test
    fun given_an_open_drawer_when_a_predictive_back_gesture_is_committed_then_it_closes() {
        val harness = setUpDrawer(initiallyOpen = true)
        val dispatcher = composeTestRule.activity.onBackPressedDispatcher

        composeTestRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(backEvent(progress = 0f))
            dispatcher.dispatchOnBackProgressed(backEvent(progress = 0.3f))
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread { dispatcher.onBackPressed() }
        composeTestRule.waitForIdle()

        harness.shouldRest(isOpen = false)
        harness.reportedChanges shouldContainExactly listOf(false)
    }

    @Test
    fun given_a_closed_drawer_then_it_leaves_back_to_the_host() {
        setUpDrawer()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.hasEnabledCallbacks() shouldBe false
        }
    }

    private fun setUpDrawer(initiallyOpen: Boolean = false): DrawerHarness {
        val harness = DrawerHarness(initiallyOpen)
        composeTestRule.setContent { DrawerHarnessContent(harness) }
        composeTestRule.waitForIdle()
        return harness
    }

    private fun backEvent(progress: Float) = BackEventCompat(
        touchX = 1000f,
        touchY = 1000f,
        progress = progress,
        swipeEdge = BackEventCompat.EDGE_RIGHT,
    )

    private companion object {
        const val PreviousRoute = "previous"
        const val HostRoute = "host"
    }
}
