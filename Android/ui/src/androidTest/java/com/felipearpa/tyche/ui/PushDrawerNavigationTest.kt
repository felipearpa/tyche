package com.felipearpa.tyche.ui

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * A destination chosen from the drawer or from the pushed screen opens once.
 *
 * The drawer learns that the host closed it, and the host that it navigated away, only when they
 * recompose, so both keep taking input until the next frame. The host here wires its actions as both
 * drawer hosts do, through [runIfStarted] on its back stack entry, so a second activation in that
 * frame does nothing, while later choices, including one made while the host fades back in, still
 * navigate.
 */
class PushDrawerNavigationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @After
    fun restoreTouchMode() {
        // The keyboard test leaves touch mode through a real key press; later tests expect it.
        runCatching { InstrumentationRegistry.getInstrumentation().setInTouchMode(true) }
    }

    @Test
    fun given_keyboard_focus_on_a_drawer_action_that_opens_a_destination_when_enter_is_pressed_twice_before_the_host_recomposes_then_it_navigates_once() {
        val host = NavigatingHost()
        composeTestRule.focusOpenerWithKeyboard()
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val focusedWhenOpen = composeTestRule.focusedNodeLabel()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val closeComposedBetweenEnters = !host.harness.reveal.targetIsOpen
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertSoftly {
            focusedWhenOpen shouldBe ProfileRowLabel
            // Both Enters reached the action: the drawer had not yet seen the close request.
            closeComposedBetweenEnters shouldBe false
            host.harness.drawerRowClicks shouldBe 2
            host.shouldShowTheDestinationOnce()
        }
    }

    @Test
    fun given_an_open_drawer_when_a_drawer_action_that_opens_a_destination_is_tapped_twice_before_the_host_recomposes_then_it_navigates_once() {
        val host = NavigatingHost()
        composeTestRule.onNodeWithTag(HarnessTags.Opener).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.tapTwiceWithinOneFrame(HarnessTags.DrawerRow)
        val closeComposedBetweenTaps = !host.harness.reveal.targetIsOpen
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertSoftly {
            // Both taps reached the action: the drawer had not yet seen the close request.
            closeComposedBetweenTaps shouldBe false
            host.harness.drawerRowClicks shouldBe 2
            host.shouldShowTheDestinationOnce()
            host.harness.shouldRest(isOpen = false)
        }
    }

    @Test
    fun given_a_closed_drawer_when_a_host_control_that_opens_a_destination_is_tapped_twice_before_the_host_recomposes_then_it_navigates_once() {
        val host = NavigatingHost()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.tapTwiceWithinOneFrame(HarnessTags.HostRow)
        val destinationComposedBetweenTaps = host.isDestinationComposed()
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertSoftly {
            // Both taps reached the host row before the navigation host composed the destination.
            destinationComposedBetweenTaps shouldBe false
            host.harness.hostRowClicks shouldBe 2
            host.shouldShowTheDestinationOnce()
        }
    }

    @Test
    fun given_a_destination_opened_from_the_drawer_when_back_returns_to_the_host_then_the_drawer_and_the_host_open_destinations_again() {
        val host = NavigatingHost()
        composeTestRule.onNodeWithTag(HarnessTags.Opener).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(HarnessTags.DrawerRow).performClick()
        composeTestRule.waitForIdle()
        Espresso.pressBack()
        composeTestRule.waitForIdle()
        val phaseOnReturn = host.harness.reveal.phase

        composeTestRule.onNodeWithTag(HarnessTags.Opener).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(HarnessTags.DrawerRow).performClick()
        composeTestRule.waitForIdle()
        val routesAfterSecondDrawerChoice = host.topRoutes()
        Espresso.pressBack()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(HarnessTags.HostRow).performClick()
        composeTestRule.waitForIdle()

        assertSoftly {
            phaseOnReturn shouldBe DrawerPhase.Closed
            routesAfterSecondDrawerChoice shouldBe listOf(HostRoute, DestinationRoute)
            host.harness.drawerRowClicks shouldBe 2
            host.harness.hostRowClicks shouldBe 1
            host.shouldShowTheDestinationOnce()
        }
    }

    /**
     * After Back, the host route is started but not yet resumed until its enter transition ends; a
     * tap in that time is a new choice and still navigates.
     */
    @Test
    fun given_the_host_fading_back_in_after_back_when_a_host_control_is_tapped_then_it_opens_the_destination() {
        val host = NavigatingHost()
        composeTestRule.onNodeWithTag(HarnessTags.HostRow).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnUiThread { host.navController.popBackStack() }
        composeTestRule.frames(count = 6) {}
        val hostStateWhenTapped = host.hostEntry.lifecycle.currentState
        composeTestRule.onNodeWithTag(HarnessTags.HostRow).performClick()
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertSoftly {
            hostStateWhenTapped shouldBe Lifecycle.State.STARTED
            host.harness.hostRowClicks shouldBe 2
            host.shouldShowTheDestinationOnce()
        }
    }

    /** The drawer harness as the start destination of a navigation host, with one destination. */
    private inner class NavigatingHost {
        val harness = DrawerHarness()
        lateinit var navController: NavHostController
        lateinit var hostEntry: NavBackStackEntry

        init {
            // As both drawer hosts wire their destinations: only while the host route is started.
            harness.onDrawerRowClick = {
                hostEntry.runIfStarted {
                    harness.isOpen = false
                    navController.navigate(DestinationRoute)
                }
            }
            harness.onHostRowClick = {
                hostEntry.runIfStarted { navController.navigate(DestinationRoute) }
            }
            composeTestRule.setContent {
                navController = rememberNavController()
                NavHost(navController = navController, startDestination = HostRoute) {
                    composable(HostRoute) { entry ->
                        hostEntry = entry
                        DrawerHarnessContent(harness)
                    }
                    composable(DestinationRoute) { Text(DestinationText) }
                }
            }
            composeTestRule.waitForIdle()
        }

        fun isDestinationComposed(): Boolean =
            composeTestRule.onAllNodesWithText(DestinationText).fetchSemanticsNodes().isNotEmpty()

        /**
         * The routes of the two topmost back stack entries, bottom first: the host and the
         * destination after one navigation, the destination twice after two.
         */
        fun topRoutes(): List<String?> = listOf(
            navController.previousBackStackEntry?.destination?.route,
            navController.currentBackStackEntry?.destination?.route,
        )

        /** The destination is on top of the host exactly once. */
        fun shouldShowTheDestinationOnce() {
            topRoutes() shouldBe listOf(HostRoute, DestinationRoute)
        }
    }

    private companion object {
        const val HostRoute = "host"
        const val DestinationRoute = "destination"
        const val DestinationText = "Destination"
    }
}
