package com.felipearpa.tyche.ui

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * The semantics assertions read the merged tree, as accessibility services do: descendants of a
 * node with cleared semantics are left out, and so are nodes that are not placed. The unmerged
 * tree keeps both for debugging, so it cannot show what TalkBack can reach. The `reachable`
 * checks read the tree an accessibility service actually receives.
 */
class PushDrawerAccessibilityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @After
    fun restoreTouchMode() {
        // The keyboard tests leave touch mode through a real key press; later tests expect it.
        runCatching { instrumentation.setInTouchMode(true) }
    }

    @Test
    fun given_a_closed_drawer_then_only_the_pushed_screen_is_exposed() {
        setUpDrawer()

        composeTestRule.onNodeWithText("Profile").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sign out").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Close menu").assertDoesNotExist()
        composeTestRule.onNodeWithTag(HarnessTags.HostRow).assertHasClickAction()
        composeTestRule.onNodeWithContentDescription("Open menu").assertHasClickAction()

        val reachable = reachableAccessibilityNodes()
        reachable.labels() shouldContainAll listOf("Open menu", "Host row", "Item 0")
        reachable.labels() shouldNotContainAnyOf DrawerLabels + "Close menu"
    }

    @Test
    fun given_an_open_drawer_then_its_actions_and_dismissal_are_exposed_and_the_pushed_screen_is_hidden() {
        setUpDrawer(initiallyOpen = true)

        composeTestRule.onNodeWithTag(HarnessTags.DrawerRow).assertHasClickAction()
        composeTestRule.onNodeWithText("Sign out").assertExists()
        composeTestRule.onNodeWithContentDescription("Close menu")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 1f))
        composeTestRule.onNodeWithTag(HarnessTags.HostRow).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Open menu").assertDoesNotExist()
        composeTestRule.onNode(hasText("Item 0")).assertDoesNotExist()

        val reachable = reachableAccessibilityNodes()
        reachable.labels() shouldContainAll DrawerLabels
        reachable.labels() shouldNotContainAnyOf HostLabels
        reachable.hasClickable("Close menu") shouldBe true
        reachable.hasClickable("Profile") shouldBe true
        // TalkBack's pane handling would restore the node last focused in the pane on reopening.
        reachable.paneTitles().shouldBeEmpty()
    }

    /**
     * TalkBack moves its cursor off the vanished opener to the first node it can reach, and keeps
     * it on a node that stays. So nothing may be reachable while the drawer opens, and once it
     * settles its first element must come before the dismissal.
     */
    @Test
    fun given_an_opening_drawer_then_nothing_is_reachable_until_it_settles_with_its_first_element_ahead_of_the_dismissal() {
        val harness = setUpDrawer()
        composeTestRule.mainClock.autoAdvance = false

        harness.isOpen = true
        val opening = composeTestRule.frames(count = 4) { harness.reveal.phase }.last()
        val reachableWhileOpening = reachableAccessibilityNodes()
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        val reachableOnceOpen = reachableAccessibilityNodes()

        opening shouldBe DrawerPhase.Opening
        reachableWhileOpening.labels().shouldBeEmpty()
        reachableWhileOpening.none { it.isClickable } shouldBe true
        harness.shouldRest(isOpen = true)
        reachableOnceOpen.labels().first() shouldBe DrawerFirstLabel
        reachableOnceOpen.labels().last() shouldBe "Close menu"
    }

    @Test
    fun given_a_drawer_dragged_open_from_closed_then_the_dismissal_is_exposed_only_once_it_settles_open() {
        val harness = setUpDrawer()

        composeTestRule.runOnIdle { harness.reveal.startDrag().translateTo(0.5f) }
        composeTestRule.waitForIdle()
        val phaseWhileDragged = harness.reveal.phase
        val dismissalWhileDragged = reachableAccessibilityNodes().labels()
        harness.isOpen = true
        composeTestRule.waitForIdle()

        phaseWhileDragged shouldBe DrawerPhase.Dragging
        dismissalWhileDragged shouldNotContain "Close menu"
        harness.shouldRest(isOpen = true)
        composeTestRule.onNodeWithContentDescription("Close menu").assertHasClickAction()
    }

    @Test
    fun given_a_closing_transition_in_flight_then_neither_drawer_actions_nor_pushed_screen_controls_are_exposed() {
        val harness = setUpDrawer(initiallyOpen = true)
        composeTestRule.mainClock.autoAdvance = false

        harness.isOpen = false
        val closing = composeTestRule.frames(count = 4) { harness.reveal.phase }.last()

        closing shouldBe DrawerPhase.Closing
        composeTestRule.onNodeWithText("Profile").assertDoesNotExist()
        composeTestRule.onNodeWithTag(HarnessTags.HostRow).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Close menu").assertHasClickAction()

        val reachable = reachableAccessibilityNodes()
        reachable.labels() shouldContain "Close menu"
        reachable.labels() shouldNotContainAnyOf DrawerLabels + HostLabels
    }

    @Test
    fun given_keyboard_focus_on_the_opener_when_the_drawer_opens_and_back_closes_it_then_focus_moves_in_and_returns() {
        val harness = setUpDrawer()
        composeTestRule.focusOpenerWithKeyboard()

        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val openedPhase = harness.reveal.phase
        composeTestRule.onNodeWithTag(HarnessTags.DrawerRow).assertIsFocused()
        Espresso.pressBack()
        composeTestRule.waitForIdle()

        openedPhase shouldBe DrawerPhase.Open
        harness.shouldRest(isOpen = false)
        composeTestRule.onNodeWithTag(HarnessTags.Opener).assertIsFocused()
    }

    @Test
    fun given_keyboard_focus_in_an_open_drawer_when_escape_is_pressed_then_it_closes_and_focus_returns_to_the_opener() {
        val harness = setUpDrawer()
        composeTestRule.focusOpenerWithKeyboard()
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val openedPhase = harness.reveal.phase

        // The system turns an unhandled Escape into Back.
        composeTestRule.pressKey(KeyEvent.KEYCODE_ESCAPE)

        openedPhase shouldBe DrawerPhase.Open
        harness.shouldRest(isOpen = false)
        harness.hostRowClicks shouldBe 0
        composeTestRule.focusedNodeLabel() shouldBe OpenerLabel
    }

    @Test
    fun given_an_open_drawer_with_keyboard_focus_when_tabbing_then_focus_never_reaches_the_pushed_screen() {
        val harness = setUpDrawer()
        composeTestRule.focusOpenerWithKeyboard()
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val openedPhase = harness.reveal.phase
        val focusedLabels = mutableListOf(composeTestRule.focusedNodeLabel())

        repeat(8) {
            composeTestRule.pressKey(KeyEvent.KEYCODE_TAB)
            focusedLabels += composeTestRule.focusedNodeLabel()
        }
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)

        openedPhase shouldBe DrawerPhase.Open
        // Tab cycles through the drawer's actions; Enter then reaches one of them, never the host.
        focusedLabels.toSet() shouldBe DrawerActionLabels.toSet()
        harness.hostRowClicks shouldBe 0
        harness.itemClicks shouldBe 0
        harness.selectedTab shouldBe 0
        harness.openerClicks shouldBe 1
    }

    /**
     * A touch opening leaves keyboard focus outside the drawer (its actions cannot take focus in
     * touch mode), and a later Tab moves it in. Closing from the keyboard must still take that
     * focus off the hidden actions: a focused control keeps receiving keys when it is not placed.
     *
     * Keyboard focus was never on the pushed screen, so there is no previous control to restore:
     * it goes to the pushed screen's default entry, the host row, which starts at the leading edge.
     */
    @Test
    fun given_a_drawer_opened_by_touch_with_keyboard_focus_tabbed_into_it_when_escape_closes_it_then_focus_moves_to_the_pushed_screen() {
        val harness = setUpDrawer()

        val result = closeTouchOpenedDrawerFromItsActions { composeTestRule.pressKey(KeyEvent.KEYCODE_ESCAPE) }

        assertSoftly {
            result.focusedAfterTouchOpening.shouldBeEmpty()
            result.focusedInDrawer shouldBeIn DrawerActionLabels
            result.focusedOnceClosed shouldContainExactly listOf(HostRowLabel)
            // Enter reached the focused host row, and no hidden action.
            harness.hostRowClicks shouldBe 1
            harness.drawerRowClicks shouldBe 0
            harness.signOutClicks shouldBe 0
            harness.shouldRest(isOpen = false)
        }
    }

    @Test
    fun given_a_drawer_opened_by_touch_with_keyboard_focus_tabbed_into_it_when_back_closes_it_then_focus_moves_to_the_pushed_screen() {
        val harness = setUpDrawer()

        val result = closeTouchOpenedDrawerFromItsActions { Espresso.pressBack() }

        assertSoftly {
            result.focusedAfterTouchOpening.shouldBeEmpty()
            result.focusedInDrawer shouldBeIn DrawerActionLabels
            result.focusedOnceClosed shouldContainExactly listOf(HostRowLabel)
            harness.hostRowClicks shouldBe 1
            harness.drawerRowClicks shouldBe 0
            harness.signOutClicks shouldBe 0
            harness.shouldRest(isOpen = false)
        }
    }

    /**
     * As in the app, where the account header sits above the menu's first action: the system's
     * first focus search after touch input starts at the window's top and prefers a control of the
     * pushed screen, whose entry is refused while the drawer is modal. The drawer itself must then
     * take focus once keyboard input begins, or keyboard focus reaches nothing at all.
     */
    @Test
    fun given_a_touch_opened_drawer_whose_first_action_lies_below_the_pushed_screen_controls_when_tab_is_pressed_then_focus_moves_to_that_action() {
        val harness = DrawerHarness()
        composeTestRule.setContent {
            DrawerHarnessContent(harness, menuTopPadding = MenuBelowPushedScreenControlsPadding)
        }
        composeTestRule.waitForIdle()
        instrumentation.setInTouchMode(true)
        composeTestRule.onNodeWithTag(HarnessTags.Opener).performClick()
        composeTestRule.waitForIdle()
        val focusedAfterTouchOpening = composeTestRule.focusedNodeLabels(useUnmergedTree = true)

        composeTestRule.pressKey(KeyEvent.KEYCODE_TAB)
        val focusedAfterTab = composeTestRule.focusedNodeLabels(useUnmergedTree = true)
        composeTestRule.pressKey(KeyEvent.KEYCODE_ESCAPE)
        val focusedOnceClosed = composeTestRule.focusedNodeLabels(useUnmergedTree = true)

        assertSoftly {
            focusedAfterTouchOpening.shouldBeEmpty()
            focusedAfterTab shouldContainExactly listOf(ProfileRowLabel)
            focusedOnceClosed shouldContainExactly listOf(HostRowLabel)
            harness.drawerRowClicks shouldBe 0
            harness.shouldRest(isOpen = false)
        }
    }

    /**
     * The caller's close request reaches the drawer a frame before the closing phase does. Keys
     * must stop reaching the chosen action from that frame on, so a quick second Enter does not run
     * it again.
     */
    @Test
    fun given_keyboard_focus_on_a_drawer_action_that_closes_it_when_enter_is_pressed_again_one_frame_later_then_the_action_runs_once() {
        val harness = DrawerHarness().apply { onDrawerRowClick = { isOpen = false } }
        composeTestRule.setContent { DrawerHarnessContent(harness) }
        composeTestRule.waitForIdle()
        composeTestRule.focusOpenerWithKeyboard()
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val focusedWhenOpen = composeTestRule.focusedNodeLabel()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        composeTestRule.frames(count = 1) {}
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val focusedAfterSecondEnter = composeTestRule.focusedNodeLabels(useUnmergedTree = true)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertSoftly {
            focusedWhenOpen shouldBe ProfileRowLabel
            focusedAfterSecondEnter.shouldBeEmpty()
            harness.drawerRowClicks shouldBe 1
            harness.shouldRest(isOpen = false)
            composeTestRule.focusedNodeLabels(useUnmergedTree = true) shouldContainExactly listOf(OpenerLabel)
        }
    }

    @Test
    fun given_keyboard_focus_on_a_drawer_action_when_the_drawer_starts_closing_then_enter_no_longer_reaches_it() {
        val harness = setUpDrawer()
        composeTestRule.focusOpenerWithKeyboard()
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val focusedWhenOpen = composeTestRule.focusedNodeLabel()
        composeTestRule.mainClock.autoAdvance = false

        harness.isOpen = false
        val closing = composeTestRule.frames(count = 4) { harness.reveal.phase }.last()
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val focusedWhileClosing = composeTestRule.focusedNodeLabels(useUnmergedTree = true)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertSoftly {
            focusedWhenOpen shouldBe ProfileRowLabel
            closing shouldBe DrawerPhase.Closing
            focusedWhileClosing.shouldBeEmpty()
            harness.drawerRowClicks shouldBe 0
            harness.shouldRest(isOpen = false)
            composeTestRule.focusedNodeLabels(useUnmergedTree = true) shouldContainExactly listOf(OpenerLabel)
        }
    }

    /**
     * As with Invite, the chosen action closes the drawer while the share sheet it opens pauses the
     * route. The route stays the current one, so focus returns to the opener once it resumes, and
     * never stays on the hidden action.
     */
    @Test
    fun given_keyboard_focus_on_a_drawer_action_that_closes_it_and_pauses_the_route_then_focus_returns_to_the_opener_once_resumed() {
        val lifecycleOwner = composeTestRule.runOnUiThread { HarnessLifecycleOwner() }
        val harness = DrawerHarness().apply {
            onDrawerRowClick = {
                isOpen = false
                lifecycleOwner.moveTo(Lifecycle.State.STARTED)
            }
        }
        composeTestRule.setContent { DrawerHarnessContent(harness, lifecycleOwner = lifecycleOwner) }
        composeTestRule.waitForIdle()
        composeTestRule.focusOpenerWithKeyboard()
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val focusedWhenOpen = composeTestRule.focusedNodeLabel()

        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val phaseWhilePaused = harness.reveal.phase
        val focusedWhilePaused = composeTestRule.focusedNodeLabels(useUnmergedTree = true)
        composeTestRule.runOnUiThread { lifecycleOwner.moveTo(Lifecycle.State.RESUMED) }
        composeTestRule.waitForIdle()
        val focusedOnceResumed = composeTestRule.focusedNodeLabels(useUnmergedTree = true)
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)

        assertSoftly {
            focusedWhenOpen shouldBe ProfileRowLabel
            phaseWhilePaused shouldBe DrawerPhase.Closed
            focusedWhilePaused.shouldBeEmpty()
            focusedOnceResumed shouldContainExactly listOf(OpenerLabel)
            // The last Enter reached the opener, not the hidden action a second time.
            harness.drawerRowClicks shouldBe 1
            harness.openerClicks shouldBe 2
            harness.shouldRest(isOpen = true)
        }
    }

    /**
     * The chosen action opens a destination. A second Enter while the drawer closes, with the host
     * still composed through the route's exit, must not run the action again, and focus must not
     * return to the host that navigated away.
     */
    @Test
    fun given_keyboard_focus_on_a_drawer_action_that_opens_a_destination_when_enter_is_pressed_again_while_closing_then_it_navigates_once() {
        val harness = DrawerHarness()
        lateinit var navController: NavHostController
        harness.onDrawerRowClick = {
            harness.isOpen = false
            navController.navigate(DestinationRoute)
        }
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = HostRoute) {
                composable(HostRoute) { DrawerHarnessContent(harness) }
                composable(DestinationRoute) { Text("Destination") }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.focusOpenerWithKeyboard()
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val focusedWhenOpen = composeTestRule.focusedNodeLabel()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val closing = composeTestRule.frames(count = 4) { harness.reveal.phase }.last()
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        val focusedWhileClosing = composeTestRule.focusedNodeLabels(useUnmergedTree = true)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertSoftly {
            focusedWhenOpen shouldBe ProfileRowLabel
            closing shouldBe DrawerPhase.Closing
            focusedWhileClosing.shouldBeEmpty()
            harness.drawerRowClicks shouldBe 1
            navController.currentDestination?.route shouldBe DestinationRoute
            navController.previousBackStackEntry?.destination?.route shouldBe HostRoute
            composeTestRule.focusedNodeLabels(useUnmergedTree = true).shouldBeEmpty()
        }
    }

    private fun setUpDrawer(initiallyOpen: Boolean = false): DrawerHarness {
        val harness = DrawerHarness(initiallyOpen)
        composeTestRule.setContent { DrawerHarnessContent(harness) }
        composeTestRule.waitForIdle()
        return harness
    }

    private class TouchOpenedDrawerResult(
        val focusedAfterTouchOpening: List<String>,
        val focusedInDrawer: String,
        val focusedOnceClosed: List<String>,
    )

    /**
     * Opens the drawer with a tap in touch mode, tabs into its actions, closes it with [close], and
     * presses Enter. The focused-node lists come from the unmerged tree, so they include focus held
     * by hidden or unplaced nodes.
     */
    private fun closeTouchOpenedDrawerFromItsActions(close: () -> Unit): TouchOpenedDrawerResult {
        instrumentation.setInTouchMode(true)
        composeTestRule.onNodeWithTag(HarnessTags.Opener).performClick()
        composeTestRule.waitForIdle()
        val focusedAfterTouchOpening = composeTestRule.focusedNodeLabels(useUnmergedTree = true)
        val focusedInDrawer = composeTestRule.tabIntoDrawer()
        close()
        composeTestRule.waitForIdle()
        val focusedOnceClosed = composeTestRule.focusedNodeLabels(useUnmergedTree = true)
        composeTestRule.pressKey(KeyEvent.KEYCODE_ENTER)
        return TouchOpenedDrawerResult(focusedAfterTouchOpening, focusedInDrawer, focusedOnceClosed)
    }

    private companion object {
        const val HostRoute = "host"
        const val DestinationRoute = "destination"
        const val DrawerFirstLabel = HarnessDrawerLabel

        /** Puts the menu's first action farther from the window's top than the host row. */
        val MenuBelowPushedScreenControlsPadding = 200.dp
        val DrawerLabels = listOf("Profile", "Sign out")
        val HostLabels = listOf("Open menu", "Host row", "Item 0", "Scores", "Chip 0", HarnessTags.HostRow)
    }
}
