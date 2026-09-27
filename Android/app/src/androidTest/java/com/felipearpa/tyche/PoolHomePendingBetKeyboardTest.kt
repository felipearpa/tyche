package com.felipearpa.tyche

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.toSize
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.pending.PendingBetList
import com.felipearpa.tyche.poolhome.PoolHomeContent
import com.felipearpa.tyche.poolhome.Tab
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import com.felipearpa.tyche.ui.R as SharedR

/**
 * Keyboard regression guard for editing a pending bet on pool home.
 *
 * Pool home keeps its tab bar at the window's bottom edge, so the keyboard covers it. The
 * pending-bet list must end at the keyboard top rather than at the keyboard top plus the tab
 * bar's height (stacked insets), bring a focused score near the end of the list above the
 * keyboard, keep the bet's Save and Cancel actions reachable, and return to its full height
 * with the draft intact once the keyboard closes.
 *
 * The window is configured like MainActivity (edge to edge, with its manifest soft-input mode),
 * and the real pool-home scaffold hosts the production pending-bet list. Only the last bet is
 * open for betting, so it is the one edited. Nothing is saved. Keyboard transitions are awaited
 * by condition, never by elapsed time.
 */
class PoolHomePendingBetKeyboardTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun configureWindowLikeMainActivity() {
        composeTestRule.runOnUiThread { composeTestRule.activity.enableEdgeToEdge() }
    }

    @Test
    fun withTheKeyboardClosedTheLastBetsActionsEndAboveTheTabs() {
        startPoolHome()

        scrollListToEnd()
        val edit = editButton().assertIsDisplayed().bounds()

        assertEquals(windowBottom(), list().bounds().bottom, PIXEL_TOLERANCE)
        assertTrue(
            "Edit (bottom ${edit.bottom}) must end above the tabs (top ${tabBarTop()})",
            edit.bottom <= tabBarTop() + PIXEL_TOLERANCE,
        )
    }

    @Test
    fun focusingAScoreNearTheEndRevealsItAboveTheKeyboardWithoutStackingTheTabBar() {
        startPoolHome()
        scrollListToEnd()
        val tabTopBeforeKeyboard = tabBarTop()
        editButton().performClick()

        awayScoreField().performClick()
        awaitListEndingAtKeyboard(visible = true)

        val field = awayScoreField().bounds()
        assertTrue(
            "Focused score (bottom ${field.bottom}) must be above the keyboard (top ${keyboardTop()})",
            field.top >= 0f && field.bottom <= keyboardTop() + PIXEL_TOLERANCE,
        )
        assertEquals(
            "The tabs keep their window-edge position under the keyboard",
            tabTopBeforeKeyboard,
            tabBarTop(),
            PIXEL_TOLERANCE,
        )

        enterDraft()
        scrollListToEnd()
        for (action in listOf(saveButton(), cancelButton())) {
            val bounds = action.assertIsDisplayed().assertIsEnabled().bounds()
            val gap = keyboardTop() - bounds.bottom
            assertTrue(
                "An action must end above the keyboard (gap $gap)",
                gap >= -PIXEL_TOLERANCE,
            )
            assertTrue(
                "The gap above the keyboard ($gap) must not include the tab bar (${tabBarHeight()})",
                gap < tabBarHeight(),
            )
        }
    }

    @Test
    fun closingTheKeyboardRestoresTheListHeightAndKeepsTheDraft() {
        startPoolHome()
        scrollListToEnd()
        editButton().performClick()
        awayScoreField().performClick()
        awaitListEndingAtKeyboard(visible = true)
        enterDraft()

        setKeyboardVisible(false)
        awaitListEndingAtKeyboard(visible = false)

        homeScoreField().assertTextEquals(DRAFT_HOME_SCORE)
        awayScoreField().assertTextEquals(DRAFT_AWAY_SCORE)
        scrollListToEnd()
        val save = saveButton().assertIsDisplayed().assertIsEnabled().bounds()
        assertTrue(
            "Save (bottom ${save.bottom}) must end above the tabs (top ${tabBarTop()})",
            save.bottom <= tabBarTop() + PIXEL_TOLERANCE,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun startPoolHome() {
        val bets = MutableStateFlow(PagingData.from(pendingBets()))
        composeTestRule.setContent {
            TycheTheme {
                PoolHomeContent(
                    selectedTabIndex = Tab.BET_EDITOR,
                    onTabChange = {},
                    isDrawerOpen = false,
                    onDrawerOpenChange = {},
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                    onPoolChange = {},
                    drawerContent = {},
                    isSaving = false,
                    content = { contentPadding ->
                        PendingBetList(
                            lazyPoolGamblerBets = bets.collectAsLazyPagingItems(),
                            contentPadding = contentPadding,
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = LOAD_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(HOME_TEAM_NAME).fetchSemanticsNodes().isNotEmpty()
        }
        applyMainActivitySoftInputMode()
        // A keyboard left open by an earlier test would cover the rows this test taps.
        setKeyboardVisible(false)
        composeTestRule.waitUntil(timeoutMillis = KEYBOARD_TIMEOUT_MILLIS) { !isKeyboardVisible() }
        composeTestRule.waitForIdle()
    }

    /** Scrolls as a gambler would to the end of the list, where its end padding applies. */
    private fun scrollListToEnd() {
        repeat(MAX_SCROLL_STEPS) {
            val range = list().fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            if (range.value() >= range.maxValue()) return
            val step = list().bounds().height
            list().performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(0f, step)
            }
            composeTestRule.waitForIdle()
        }
        error("The list did not reach its end in $MAX_SCROLL_STEPS steps")
    }

    /**
     * Gives the attached window MainActivity's manifest soft-input mode. Set before the content
     * is attached, the mode does not reach the window manager, which then pans the window.
     */
    private fun applyMainActivitySoftInputMode() {
        composeTestRule.runOnUiThread {
            val activity = composeTestRule.activity
            val mainActivityInfo = activity.packageManager.getActivityInfo(
                ComponentName(activity, MainActivity::class.java),
                0,
            )
            activity.window.setSoftInputMode(mainActivityInfo.softInputMode)
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Waits until the keyboard reaches the requested state and the list's viewport has settled
     * at the keyboard top, or at the window bottom once the keyboard is closed. The viewport
     * itself must end there: a scrollable brings a focused field back into view only when its
     * viewport shrinks past the field, not when padding or a covering bar hides it.
     */
    private fun awaitListEndingAtKeyboard(visible: Boolean) {
        runCatching {
            composeTestRule.waitUntil(timeoutMillis = KEYBOARD_TIMEOUT_MILLIS) {
                isKeyboardVisible() == visible &&
                    abs(list().bounds().bottom - expectedListBottom()) <= PIXEL_TOLERANCE
            }
        }
        composeTestRule.waitForIdle()
        assertEquals("Keyboard visibility", visible, isKeyboardVisible())
        assertEquals(
            "List viewport bottom (keyboard ${imeHeight()}, tab bar ${tabBarHeight()})",
            expectedListBottom(),
            list().bounds().bottom,
            PIXEL_TOLERANCE,
        )
    }

    private fun expectedListBottom() = keyboardTop()

    private fun setKeyboardVisible(visible: Boolean) {
        composeTestRule.runOnUiThread {
            val window = composeTestRule.activity.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            if (visible) {
                controller.show(WindowInsetsCompat.Type.ime())
            } else {
                controller.hide(WindowInsetsCompat.Type.ime())
            }
        }
    }

    private fun rootInsets(): WindowInsetsCompat = composeTestRule.runOnUiThread {
        requireNotNull(ViewCompat.getRootWindowInsets(composeTestRule.activity.window.decorView))
    }

    private fun isKeyboardVisible() = rootInsets().isVisible(WindowInsetsCompat.Type.ime())

    private fun imeHeight() =
        rootInsets().getInsets(WindowInsetsCompat.Type.ime()).bottom.toFloat()

    private fun keyboardTop() = windowBottom() - imeHeight()

    private fun windowBottom() = composeTestRule.onRoot().fetchSemanticsNode().boundsInWindow.bottom

    private fun tabBarTop() = selectedTab().bounds().top

    private fun tabBarHeight() = windowBottom() - tabBarTop()

    /** Unclipped window bounds: a node scrolled out of view keeps its real position. */
    private fun SemanticsNodeInteraction.bounds(): Rect {
        val node = fetchSemanticsNode()
        return Rect(offset = node.positionInWindow, size = node.size.toSize())
    }

    private fun list() = composeTestRule.onNode(hasScrollToIndexAction())

    /** The Bets tab; the top app bar shows the same text as its title. */
    private fun selectedTab() = composeTestRule.onNode(
        hasText(string(R.string.bet_tab)) and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected),
    )

    private fun editButton() = composeTestRule.onNodeWithText(string(SharedR.string.edit_action))

    private fun saveButton() = composeTestRule.onNodeWithText(string(SharedR.string.save_action))

    private fun cancelButton() =
        composeTestRule.onNodeWithText(string(SharedR.string.cancel_action))

    /** A complete score, so Save is enabled; the away field keeps focus. */
    private fun enterDraft() {
        homeScoreField().performTextReplacement(DRAFT_HOME_SCORE)
        awayScoreField().performTextReplacement(DRAFT_AWAY_SCORE)
    }

    private fun homeScoreField() = composeTestRule.onAllNodes(hasSetTextAction())[0]

    /** The lower of the two score fields of the bet being edited. */
    private fun awayScoreField() = composeTestRule.onAllNodes(hasSetTextAction())[1]

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val DRAFT_HOME_SCORE = "2"
        const val DRAFT_AWAY_SCORE = "7"
        const val HOME_TEAM_NAME = "Argentina"
        const val BET_COUNT = 20
        const val MAX_SCROLL_STEPS = 50
        const val PIXEL_TOLERANCE = 1.5f
        const val LOAD_TIMEOUT_MILLIS = 5_000L
        const val KEYBOARD_TIMEOUT_MILLIS = 15_000L

        /** Locked bets except the last, which is open for betting and has no score yet. */
        fun pendingBets() = List(BET_COUNT) { index ->
            val isLast = index == BET_COUNT - 1
            PoolGamblerBetModel(
                poolId = "pool",
                gamblerId = "gambler",
                gamblerUsername = "gambler",
                matchId = "match$index",
                homeTeamId = "ar",
                homeTeamName = HOME_TEAM_NAME,
                awayTeamId = "uy",
                awayTeamName = "Uruguay",
                matchScore = null,
                betScore = null,
                score = null,
                matchDateTime = LocalDateTime(2026, 10, 1 + index / 4, 12, 0),
                isLocked = !isLast,
                isComputed = false,
            )
        }
    }
}
