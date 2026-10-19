package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.Draft
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.RowState
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.Team
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import org.junit.Test

/**
 * The Bets card in each state as the gambler and TalkBack meet it: its status, its controls, the
 * score boxes' input and focus, and the fixed status height. Runs in English.
 */
class PendingBetItemPresentationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun eachStateShowsItsStatusAndOnlyItsControls() {
        var state by mutableStateOf(Shown(RowState.EMPTY, Draft(null, null)))
        composeTestRule.setContent {
            TycheTheme {
                PendingBetItem(
                    match = if (state.rowState == RowState.LOCKED) match.copy(isLocked = true) else match,
                    draft = state.draft,
                    rowState = state.rowState,
                    lockCountdown = null,
                    actions = recordingActions(),
                )
            }
        }

        expectations.forEach { expected ->
            state = expected.shown
            composeTestRule.waitForIdle()

            summary().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected.status))
            if (expected.shown.rowState != RowState.SAVING) composeTestRule.onNodeWithText(expected.status, useUnmergedTree = true).assertExists()
            controlCount("One more goal for Colombia") shouldBe if (expected.hasSteppers) 1 else 0
            composeTestRule.onAllNodesWithText("Add Portugal's score to save", useUnmergedTree = true).fetchSemanticsNodes().size shouldBe 0
            composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().size shouldBe
                if (expected.shown.rowState == RowState.FAILED) 1 else 0
            composeTestRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size shouldBe
                if (expected.hasSteppers) 2 else 0
        }
    }

    @Test
    fun aRefusedSaveTurnsTheCardIntoTheLockedSavedPrediction() {
        var shown by mutableStateOf(Shown(RowState.SAVING, Draft(2, 1)))
        composeTestRule.setContent {
            TycheTheme {
                PendingBetItem(
                    match = if (shown.rowState == RowState.LOCKED) match.copy(isLocked = true) else match,
                    draft = shown.draft,
                    rowState = shown.rowState,
                    lockCountdown = null,
                    actions = recordingActions(),
                    announcement = if (shown.rowState == RowState.LOCKED) "Betting on Colombia versus Portugal is now closed" else null,
                )
            }
        }

        shown = Shown(RowState.LOCKED, Draft(1, 0))
        composeTestRule.waitForIdle()

        summary().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Locked"))
        composeTestRule.onNode(hasContentDescription("Betting on Colombia versus Portugal is now closed")).assertExists()
        controlCount("One more goal for Colombia") shouldBe 0
        composeTestRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        readOnlyBox("Colombia goals").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1"))
        readOnlyBox("Portugal goals").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "0"))
    }

    @Test
    fun anOpenCardKeepsOneHeightFromNoBetThroughIncompleteSavingAndSaved() {
        var shown by mutableStateOf(Shown(RowState.EMPTY, Draft(null, null)))
        composeTestRule.setContent {
            TycheTheme {
                PendingBetItem(
                    match = match,
                    draft = shown.draft,
                    rowState = shown.rowState,
                    lockCountdown = null,
                    actions = recordingActions(),
                    modifier = Modifier.testTag(CARD_TAG),
                )
            }
        }

        val heights = listOf(
            Shown(RowState.EMPTY, Draft(null, null)),
            Shown(RowState.INCOMPLETE, Draft(1, null)),
            Shown(RowState.EMPTY, Draft(1, 0)),
            Shown(RowState.SAVING, Draft(1, 0)),
            Shown(RowState.SAVED, Draft(1, 0)),
        ).map { next ->
            shown = next
            composeTestRule.waitForIdle()
            cardHeight()
        }

        heights.distinct() shouldBe listOf(heights.first())
    }

    @Test
    fun theMissingBoxOfAnIncompleteCardSaysWhoseScoreIsNeededToSave() {
        setCard(draft = Draft(2, null), rowState = RowState.INCOMPLETE, actions = recordingActions())

        awayBox().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Add Portugal's score to save"))
        homeBox().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun aLockedCardWithoutAPredictionShowsDashesAndTakesNoInput() {
        setCard(match = match.copy(isLocked = true), draft = Draft(null, null), rowState = RowState.LOCKED)

        // The muted dash is drawn, not read: each box speaks its team and "No score".
        readOnlyBox("Colombia goals").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "No score"))
        readOnlyBox("Portugal goals").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "No score"))
        composeTestRule.onAllNodesWithText("?", useUnmergedTree = true).assertCountEquals(0)
        composeTestRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun typingKeepsDigitsDropsLeadingZerosCapsTwoDigitsAndMovesFocusToAway() {
        val recorded = Recorded()
        var draft by mutableStateOf(Draft(null, null))
        composeTestRule.setContent {
            TycheTheme {
                PendingBetItem(
                    match = match,
                    draft = draft,
                    rowState = RowState.EMPTY,
                    lockCountdown = null,
                    actions = recordingActions(
                        recorded = recorded,
                        setHome = { value -> draft = draft.copy(home = value) },
                        setAway = { value -> draft = draft.copy(away = value) },
                    ),
                )
            }
        }

        homeBox().performClick()
        homeBox().performTextInput("0a2")
        composeTestRule.waitForIdle()

        draft.home shouldBe 2
        homeBox().assertIsNotFocused()
        awayBox().assertIsFocused()
        recorded.focusLeaves shouldBe 0

        awayBox().performTextInput("123")
        composeTestRule.waitForIdle()

        draft shouldBe Draft(home = 2, away = 12)
        recorded.focusLeaves shouldBe 0
    }

    @Test
    fun typingTheSameDigitOverTheSelectedHomeScoreStillMovesFocusToAway() {
        val recorded = Recorded()
        var draft by mutableStateOf(Draft(2, 1))
        composeTestRule.setContent {
            TycheTheme {
                PendingBetItem(
                    match = match,
                    draft = draft,
                    rowState = RowState.SAVED,
                    lockCountdown = null,
                    actions = recordingActions(
                        recorded = recorded,
                        setHome = { value -> draft = draft.copy(home = value) },
                        setAway = { value -> draft = draft.copy(away = value) },
                    ),
                )
            }
        }

        homeBox().performClick()
        composeTestRule.waitForIdle()
        homeBox().performTextInput("2")
        composeTestRule.waitForIdle()

        awayBox().assertIsFocused()
        awayBox().performTextInput("3")
        composeTestRule.waitForIdle()
        draft shouldBe Draft(home = 2, away = 3)
    }

    @Test
    fun leavingBothBoxesReportsFocusLeaveOnce() {
        val recorded = Recorded()
        lateinit var focusManager: FocusManager
        composeTestRule.setContent {
            focusManager = LocalFocusManager.current
            TycheTheme {
                PendingBetItem(
                    match = match,
                    draft = Draft(2, 1),
                    rowState = RowState.SAVED,
                    lockCountdown = null,
                    actions = recordingActions(recorded = recorded),
                )
            }
        }

        homeBox().performClick()
        awayBox().performClick()
        composeTestRule.waitForIdle()
        recorded.focusLeaves shouldBe 0

        composeTestRule.runOnIdle { focusManager.clearFocus() }
        composeTestRule.waitForIdle()
        recorded.focusLeaves shouldBe 1
    }

    @Test
    fun steppersReachTheirActions() {
        val recorded = Recorded()
        setCard(draft = Draft(null, null), rowState = RowState.EMPTY, actions = recordingActions(recorded = recorded))

        composeTestRule.onNodeWithContentDescription("One more goal for Portugal").performClick()
        composeTestRule.onNodeWithContentDescription("One less goal for Colombia").performClick()

        recorded.steps shouldBe listOf(Team.AWAY to 1, Team.HOME to -1)
    }

    @Test
    fun theStatusKeepsItsHeightFromSavingToSaved() {
        var rowState by mutableStateOf(RowState.SAVING)
        composeTestRule.setContent {
            TycheTheme {
                PendingBetItem(match = match, draft = Draft(2, 1), rowState = rowState, lockCountdown = null, actions = recordingActions())
            }
        }
        composeTestRule.mainClock.autoAdvance = false
        val savingHeight = statusHeight()

        rowState = RowState.SAVED
        composeTestRule.mainClock.advanceTimeBy(PendingBetStyle.ANIMATION_MILLIS / 2L)
        val midwayHeight = statusHeight()
        composeTestRule.mainClock.advanceTimeBy(PendingBetStyle.ANIMATION_MILLIS * 2L)
        val savedHeight = statusHeight()
        // A paused clock keeps the activity from finishing its teardown.
        composeTestRule.mainClock.autoAdvance = true

        midwayHeight shouldBe savingHeight
        savedHeight shouldBe savingHeight
        savedHeight shouldBe 26.dp
    }

    @Test
    fun theStateDescriptionMovesFromSavingToSaved() {
        var rowState by mutableStateOf(RowState.SAVING)
        composeTestRule.setContent {
            TycheTheme {
                PendingBetItem(match = match, draft = Draft(2, 1), rowState = rowState, lockCountdown = null, actions = recordingActions())
            }
        }
        summary().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saving"))

        rowState = RowState.SAVED

        summary().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saved"))
    }

    @Test
    fun theLockNoteJoinsTheSummary() {
        setCard(draft = Draft(null, null), rowState = RowState.EMPTY, lockCountdown = 8)

        composeTestRule.onNodeWithText("Locks in 8 min", useUnmergedTree = true).assertExists()
        summary()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Colombia versus Portugal, 2:00 PM, Locks in 8 min")))
    }

    @Test
    fun aPlaceholderCardHasNoClickableOrReadableNodes() {
        composeTestRule.setContent {
            TycheTheme {
                Column {
                    PendingBetPlaceholderItem(modifier = Modifier.fillMaxWidth().testTag(PLACEHOLDER_TAG))
                }
            }
        }

        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        composeTestRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        composeTestRule.onAllNodes(hasText("X", substring = true)).assertCountEquals(0)
        composeTestRule.onAllNodes(hasText("88", substring = true)).assertCountEquals(0)
        composeTestRule.onAllNodes(hasText("Locked")).assertCountEquals(0)
    }

    private fun setCard(
        match: PoolGamblerBetModel = this.match,
        draft: Draft,
        rowState: RowState,
        lockCountdown: Int? = null,
        actions: PendingBetItemActions? = null,
    ) {
        composeTestRule.setContent {
            TycheTheme {
                PendingBetItem(
                    match = match,
                    draft = draft,
                    rowState = rowState,
                    lockCountdown = lockCountdown,
                    actions = actions,
                )
            }
        }
    }

    /** The kickoff summary; the lock announcement also names the matchup, without the comma. */
    private fun summary() = composeTestRule.onNode(hasContentDescription("Colombia versus Portugal, ", substring = true))

    private fun readOnlyBox(label: String) = composeTestRule.onNode(hasContentDescription(label))

    private fun homeBox() = composeTestRule.onNode(hasSetTextAction() and hasContentDescription("Colombia goals"))

    private fun awayBox() = composeTestRule.onNode(hasSetTextAction() and hasContentDescription("Portugal goals"))

    private fun controlCount(description: String) =
        composeTestRule.onAllNodes(hasContentDescription(description) and hasClickAction()).fetchSemanticsNodes().size

    private fun cardHeight() = composeTestRule.onNodeWithTag(CARD_TAG, useUnmergedTree = true)
        .getUnclippedBoundsInRoot().let { bounds -> bounds.bottom - bounds.top }

    private fun statusHeight() = composeTestRule.onNodeWithTag(PENDING_BET_STATUS_TAG, useUnmergedTree = true)
        .getUnclippedBoundsInRoot().let { bounds -> bounds.bottom - bounds.top }

    private class Recorded {
        var focusLeaves = 0
        var steps: List<Pair<Team, Int>> = emptyList()
    }

    private fun recordingActions(
        recorded: Recorded = Recorded(),
        setHome: (Int?) -> Unit = {},
        setAway: (Int?) -> Unit = {},
    ) = PendingBetItemActions(
        setHome = setHome,
        setAway = setAway,
        step = { team, delta -> recorded.steps = recorded.steps + (team to delta) },
        retry = {},
        focusLeave = { recorded.focusLeaves++ },
    )

    private data class Shown(val rowState: RowState, val draft: Draft)

    private data class Expected(
        val shown: Shown,
        val status: String,
        val hasSteppers: Boolean,
    )

    private val expectations = listOf(
        Expected(Shown(RowState.EMPTY, Draft(null, null)), "No bet", hasSteppers = true),
        Expected(Shown(RowState.INCOMPLETE, Draft(2, null)), "Incomplete", hasSteppers = true),
        Expected(Shown(RowState.SAVING, Draft(2, 1)), "Saving", hasSteppers = true),
        Expected(Shown(RowState.SAVED, Draft(2, 1)), "Saved", hasSteppers = true),
        Expected(Shown(RowState.FAILED, Draft(2, 1)), "Not saved", hasSteppers = true),
        Expected(Shown(RowState.LOCKED, Draft(2, 0)), "Locked", hasSteppers = false),
    )

    private val match = PoolGamblerBetModel(
        poolId = "pool",
        gamblerId = "gambler",
        gamblerUsername = "gambler",
        matchId = "match",
        homeTeamId = "co",
        homeTeamName = "Colombia",
        awayTeamId = "pt",
        awayTeamName = "Portugal",
        matchScore = null,
        betScore = null,
        score = null,
        matchDateTime = LocalDateTime(year = 2026, month = 10, day = 21, hour = 14, minute = 0),
        isLocked = false,
        isComputed = false,
    )
}

private const val CARD_TAG = "pendingBetCard"

private const val PLACEHOLDER_TAG = "pendingBetPlaceholder"
