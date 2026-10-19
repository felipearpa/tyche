package com.felipearpa.tyche

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.unit.toSize
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.felipearpa.tyche.bet.pending.PendingBetListView
import com.felipearpa.tyche.bet.pending.PendingBetListViewModel
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.bet.application.GetPendingPoolGamblerBets
import com.felipearpa.tyche.data.bet.application.PlaceBet
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository
import com.felipearpa.tyche.data.bet.domain.Bet
import com.felipearpa.tyche.data.bet.domain.KickoffGroup
import java.io.IOException
import com.felipearpa.tyche.poolhome.PoolHomeContent
import com.felipearpa.tyche.poolhome.Tab
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import com.felipearpa.tyche.ui.R as SharedR

/**
 * Keyboard regression guard for entering a prediction on pool home.
 *
 * Pool home keeps its tab bar at the window's bottom edge, so the keyboard covers it. The
 * pending-bet list must end at the keyboard top rather than at the keyboard top plus the tab
 * bar's height (stacked insets), bring the whole card of a focused score near the end of the list
 * above the keyboard (both score boxes and their steppers), keep it there when focus moves from
 * the home box to the away box, keep a failed save's Retry reachable, and return to its full
 * height with the draft intact once the keyboard closes.
 *
 * The window is configured like MainActivity (edge to edge, with its manifest soft-input mode),
 * and the real pool-home scaffold hosts the production Bets list and view model over a mocked
 * repository whose saves fail, so nothing reaches a backend. Only the last match is open for
 * betting, so it is the one edited. Keyboard transitions are awaited by condition, never by
 * elapsed time.
 */
class PoolHomePendingBetKeyboardTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun configureWindowLikeMainActivity() {
        composeTestRule.runOnUiThread { composeTestRule.activity.enableEdgeToEdge() }
    }

    @Test
    fun withTheKeyboardClosedTheLastCardsControlsEndAboveTheTabs() {
        startPoolHome()

        scrollListToEnd()
        val stepper = lowerStepper(AWAY_TEAM_NAME).assertIsDisplayed().bounds()

        assertEquals(windowBottom(), list().bounds().bottom, PIXEL_TOLERANCE)
        assertTrue(
            "The last stepper (bottom ${stepper.bottom}) must end above the tabs (top ${tabBarTop()})",
            stepper.bottom <= tabBarTop() + PIXEL_TOLERANCE,
        )
    }

    @Test
    fun focusingAScoreNearTheEndRevealsTheWholeCardAboveTheKeyboardWithoutStackingTheTabBar() {
        startPoolHome()
        scrollListToEnd()
        val tabTopBeforeKeyboard = tabBarTop()

        homeScoreBox().performClick()
        awaitListEndingAtKeyboard(visible = true)

        assertCardAboveKeyboard()
        assertEquals(
            "The tabs keep their window-edge position under the keyboard",
            tabTopBeforeKeyboard,
            tabBarTop(),
            PIXEL_TOLERANCE,
        )

        homeScoreBox().performTextInput(DRAFT_HOME_SCORE)
        composeTestRule.waitUntil(timeoutMillis = KEYBOARD_TIMEOUT_MILLIS) { isFocused(awayScoreBox()) }
        composeTestRule.waitForIdle()
        assertCardAboveKeyboard()
    }

    @Test
    fun aFailedSavesRetryIsReachableAboveTheKeyboard() {
        startPoolHome()
        scrollListToEnd()
        homeScoreBox().performClick()
        awaitListEndingAtKeyboard(visible = true)

        enterDraft()
        awaitRetry()
        scrollListToEnd()

        val retry = retryButton().assertIsDisplayed().bounds()
        val gap = keyboardTop() - retry.bottom
        assertTrue("Retry must end above the keyboard (gap $gap)", gap >= -PIXEL_TOLERANCE)
        assertTrue(
            "The gap above the keyboard ($gap) must not include the tab bar (${tabBarHeight()})",
            gap < tabBarHeight(),
        )
    }

    @Test
    fun closingTheKeyboardRestoresTheListHeightAndKeepsTheDraft() {
        startPoolHome()
        scrollListToEnd()
        homeScoreBox().performClick()
        awaitListEndingAtKeyboard(visible = true)
        enterDraft()
        awaitRetry()

        setKeyboardVisible(false)
        awaitListEndingAtKeyboard(visible = false)

        homeScoreBox().assertTextEquals(DRAFT_HOME_SCORE)
        awayScoreBox().assertTextEquals(DRAFT_AWAY_SCORE)
        scrollListToEnd()
        val retry = retryButton().assertIsDisplayed().bounds()
        assertTrue(
            "Retry (bottom ${retry.bottom}) must end above the tabs (top ${tabBarTop()})",
            retry.bottom <= tabBarTop() + PIXEL_TOLERANCE,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun startPoolHome() {
        val viewModel = PendingBetListViewModel(
            poolId = POOL_ID,
            gamblerId = GAMBLER_ID,
            getPendingPoolGamblerBets = GetPendingPoolGamblerBets(failingSaveRepository()),
            placeBet = PlaceBet(failingSaveRepository()),
        )
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
                        PendingBetListView(viewModel = viewModel, contentPadding = contentPadding)
                    },
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = LOAD_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodes(hasContentDescription(HOME_TEAM_NAME, substring = true)).fetchSemanticsNodes().isNotEmpty()
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

    /** Both score boxes and the steppers of the open card end above the keyboard, below the top. */
    private fun assertCardAboveKeyboard() {
        val controls = listOf(
            homeScoreBox(),
            awayScoreBox(),
            upperStepper(HOME_TEAM_NAME),
            upperStepper(AWAY_TEAM_NAME),
            lowerStepper(HOME_TEAM_NAME),
            lowerStepper(AWAY_TEAM_NAME),
        )
        for (control in controls) {
            val bounds = control.bounds()
            assertTrue(
                "A card control ($bounds) must be visible above the keyboard (top ${keyboardTop()})",
                bounds.top >= list().bounds().top - PIXEL_TOLERANCE && bounds.bottom <= keyboardTop() + PIXEL_TOLERANCE,
            )
        }
    }

    private fun awaitRetry() {
        composeTestRule.waitUntil(timeoutMillis = SAVE_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodes(hasText(string(SharedR.string.retry_action))).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    private fun isFocused(node: SemanticsNodeInteraction) =
        node.fetchSemanticsNode().config.getOrElse(SemanticsProperties.Focused) { false }

    private fun retryButton() = composeTestRule.onNode(hasText(string(SharedR.string.retry_action)))

    private fun upperStepper(teamName: String) =
        composeTestRule.onNode(hasContentDescription("One more goal for $teamName") and hasClickAction())

    private fun lowerStepper(teamName: String) =
        composeTestRule.onNode(hasContentDescription("One less goal for $teamName") and hasClickAction())

    /** A complete score, saved automatically; the mocked save fails. The away box keeps focus. */
    private fun enterDraft() {
        homeScoreBox().performTextInput(DRAFT_HOME_SCORE)
        composeTestRule.waitUntil(timeoutMillis = KEYBOARD_TIMEOUT_MILLIS) { isFocused(awayScoreBox()) }
        awayScoreBox().performTextInput(DRAFT_AWAY_SCORE)
    }

    private fun homeScoreBox() =
        composeTestRule.onNode(hasSetTextAction() and hasContentDescription("$HOME_TEAM_NAME goals"))

    private fun awayScoreBox() =
        composeTestRule.onNode(hasSetTextAction() and hasContentDescription("$AWAY_TEAM_NAME goals"))

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private companion object {
        const val DRAFT_HOME_SCORE = "2"
        const val DRAFT_AWAY_SCORE = "7"
        const val HOME_TEAM_NAME = "Argentina"
        const val AWAY_TEAM_NAME = "Uruguay"
        const val POOL_ID = "pool"
        const val GAMBLER_ID = "gambler"
        const val BET_COUNT = 20
        const val MAX_SCROLL_STEPS = 50
        const val PIXEL_TOLERANCE = 1.5f
        const val LOAD_TIMEOUT_MILLIS = 20_000L
        const val KEYBOARD_TIMEOUT_MILLIS = 15_000L
        const val SAVE_TIMEOUT_MILLIS = 10_000L

        fun failingSaveRepository(): PoolGamblerBetRepository = FailingSaveRepository(pendingBets())

        /** Locked bets except the last, which is open for betting and has no score yet. */
        fun pendingBets() = List(BET_COUNT) { index ->
            val isLast = index == BET_COUNT - 1
            PoolGamblerBet(
                poolId = POOL_ID,
                gamblerId = GAMBLER_ID,
                gamblerUsername = "gambler",
                matchId = "match$index",
                homeTeamId = "ar",
                homeTeamName = HOME_TEAM_NAME,
                awayTeamId = "uy",
                awayTeamName = AWAY_TEAM_NAME,
                awayTeamScore = null,
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

/** Serves one page of pending bets and fails every save, so the edited card shows Retry. */
private class FailingSaveRepository(private val pendingBets: List<PoolGamblerBet>) : PoolGamblerBetRepository {
    override suspend fun bet(bet: Bet): Result<PoolGamblerBet> = Result.failure(IOException("offline"))

    override suspend fun getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerBet>> = Result.success(CursorPage(items = pendingBets, next = null))

    override suspend fun getFinishedPoolGamblerBets(poolId: String, gamblerId: String, next: String?): Result<CursorPage<KickoffGroup>> =
        Result.failure(UnsupportedOperationException())

    override suspend fun getLivePoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerBet>> = Result.failure(UnsupportedOperationException())

    override suspend fun getRankedPoolMatchGamblerBets(poolId: String, matchId: String, next: String?): Result<CursorPage<PoolGamblerBet>> =
        Result.failure(UnsupportedOperationException())

    override suspend fun getPoolGamblerBet(poolId: String, gamblerId: String, matchId: String): Result<PoolGamblerBet> =
        Result.failure(UnsupportedOperationException())

    override suspend fun getGamblerBetsTimeline(poolId: String, gamblerId: String, next: String?): Result<CursorPage<KickoffGroup>> =
        Result.failure(UnsupportedOperationException())
}
