package com.felipearpa.tyche.bet.match

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.MATCH_PREVIEW_VIEWER_ID
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.HistoryMatchDateFormat
import com.felipearpa.tyche.bet.finished.Localized
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.bet.matchGamblerBetPlaceholderModel
import com.felipearpa.tyche.bet.matchParticipantPreviewModels
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * The match screen's heading, participant rows, hidden state, and list as TalkBack and touch use
 * them. Each real heading, row, and own prediction is one announcement; placeholders expose
 * nothing and do nothing.
 */
class MatchBetsPresentationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun anotherParticipantsRowAnnouncesIdentityPredictionAndAwardAndOpensOnce() {
        var opened = 0
        setLocalized(Locale.US) {
            MatchGamblerBetItem(
                poolGamblerBet = participants(isComputed = true).first { it.gamblerId == "preview-3" },
                onClick = { opened++ },
                modifier = Modifier.fillMaxWidth().testTag(ROW_TAG),
            )
        }

        description(ROW_TAG) shouldBe "Juano, Bet: España 1, Argentina 0, 4 points"
        composeTestRule.onNodeWithTag(ROW_TAG).assertHasClickAction().performClick()
        opened shouldBe 1
    }

    @Test
    fun aCanceledPressDoesNotOpenTheTimeline() {
        var opened = 0
        setLocalized(Locale.US) {
            MatchGamblerBetItem(
                poolGamblerBet = participants(isComputed = true).first(),
                onClick = { opened++ },
                modifier = Modifier.fillMaxWidth().testTag(ROW_TAG),
            )
        }

        // A press that slides off the row before release is canceled, as is a press the system cancels.
        composeTestRule.onNodeWithTag(ROW_TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, height * 3f))
            up()
        }
        composeTestRule.onNodeWithTag(ROW_TAG).performTouchInput {
            down(center)
            cancel()
        }
        composeTestRule.waitForIdle()
        opened shouldBe 0
    }

    @Test
    fun theSignedInRowSaysYouAndHasNoAction() {
        setLocalized(Locale.US) {
            MatchGamblerBetItem(
                poolGamblerBet = participants(isComputed = true).first { it.gamblerId == MATCH_PREVIEW_VIEWER_ID },
                isCurrentUser = true,
                modifier = Modifier.fillMaxWidth().testTag(ROW_TAG),
            )
        }

        description(ROW_TAG) shouldBe "felipearpa, You, Bet: España 3, Argentina 2, 0 points"
        composeTestRule.onNodeWithTag(ROW_TAG).assertHasNoClickAction()
        composeTestRule.onNodeWithTag(ROW_TAG).assert(hasNoRole())
    }

    @Test
    fun aMissingPredictionAndAnUnavailableAwardAreNotReadAsZero() {
        setLocalized(Locale.US) {
            MatchGamblerBetItem(
                poolGamblerBet = participants(isComputed = true).last(),
                modifier = Modifier.fillMaxWidth().testTag(ROW_TAG),
            )
        }

        description(ROW_TAG) shouldBe "Juan Pablo Rojas, No bet, Points unavailable"
    }

    @Test
    fun anUncomputedRowAnnouncesNoAward() {
        setLocalized(Locale.US) {
            MatchGamblerBetItem(
                poolGamblerBet = participants(isComputed = false).first { it.gamblerId == "preview-3" },
                modifier = Modifier.fillMaxWidth().testTag(ROW_TAG),
            )
        }

        description(ROW_TAG) shouldBe "Juano, Bet: España 1, Argentina 0"
        composeTestRule.onAllNodesWithText("pts", substring = true).assertCountEquals(0)
    }

    @Test
    fun oneSpanishPointIsSingular() {
        setLocalized(SPANISH) {
            MatchGamblerBetItem(
                poolGamblerBet = participants(isComputed = true).first().copy(betScore = TeamScore(1, 0), score = 1),
                modifier = Modifier.fillMaxWidth().testTag(ROW_TAG),
            )
        }

        val description = description(ROW_TAG)
        description shouldContain "Apuesta: España 1, Argentina 0"
        description shouldContain ", 1 punto"
        description shouldNotContain "1 puntos"
    }

    @Test
    fun theHeadingIsOneAnnouncementForEachState() {
        val base = historyBetPreviewModels()[1]
        val cases = listOf(
            base.copy(isLocked = false, isComputed = false) to MatchBetsPresentation.HIDDEN,
            base.copy(isComputed = false, matchScore = null) to MatchBetsPresentation.BETS_CLOSED,
            base.copy(isComputed = false, matchScore = TeamScore(1, 0)) to MatchBetsPresentation.BETS_CLOSED,
            base to MatchBetsPresentation.COMPUTED,
            base.copy(matchScore = null) to MatchBetsPresentation.COMPUTED,
        )
        setLocalized(Locale.US) {
            Column {
                cases.forEachIndexed { index, (match, presentation) ->
                    MatchHeader(
                        match = match,
                        presentation = presentation,
                        dateFormat = HistoryMatchDateFormat(Locale.US, is24HourClock = false, today = TODAY),
                        modifier = Modifier.testTag("$HEADER_TAG$index"),
                    )
                }
            }
        }

        cases.indices.map { description("$HEADER_TAG$it") } shouldBe listOf(
            "España versus Argentina. Starts Sunday, Jul 19 · 2:00 PM",
            "España versus Argentina. Bets closed",
            "Match score: España 1, Argentina 0. Bets closed",
            "Final score: España 0, Argentina 0. Sunday, Jul 19 · 2:00 PM",
            "España versus Argentina, final score unavailable. Sunday, Jul 19 · 2:00 PM",
        )
        composeTestRule.onAllNodesWithText("In progress", substring = true).assertCountEquals(0)
    }

    @Test
    fun theHiddenStateShowsOnlyTheViewersOwnPrediction() {
        val match = historyBetPreviewModels()[1].copy(isLocked = false, isComputed = false)
        setLocalized(Locale.US) {
            MatchHiddenBetsContent(match = match, ownBet = OwnBetState.Bet(TeamScore(3, 2)), onOwnBetRetry = {})
        }

        composeTestRule.onNodeWithContentDescription("Your bet: España 3, Argentina 2").assertExists()
        composeTestRule.onNodeWithText("Bets are hidden", substring = true).assertExists()
        composeTestRule
            .onNodeWithText("Everyone's bets are revealed when betting closes for this match.", substring = true)
            .assertExists()
    }

    @Test
    fun theOwnPredictionKeepsLoadingFailureAndAbsenceDistinct() {
        var retries = 0
        val match = historyBetPreviewModels()[1].copy(isLocked = false, isComputed = false)
        setLocalized(Locale.US) {
            Column {
                MatchOwnBetRegion(match = match, state = OwnBetState.Loading, onRetry = {}, modifier = Modifier.testTag(LOADING_TAG))
                MatchOwnBetRegion(match = match, state = OwnBetState.NoBet, onRetry = {})
                MatchOwnBetRegion(match = match, state = OwnBetState.Failed, onRetry = { retries++ })
            }
        }

        composeTestRule.onNodeWithTag(LOADING_TAG).assert(hasNoContentDescription())
        composeTestRule.onAllNodesWithText("0 – 0", substring = true).assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription("No bet placed").assertExists()
        composeTestRule.onNodeWithText("Couldn't load your bet.").assertExists()
        composeTestRule.onNodeWithContentDescription("Retry").performClick()
        retries shouldBe 1
    }

    @Test
    fun theListPutsTheHeadingFirstAndOpensOnlyOtherParticipants() {
        val opened = mutableListOf<String>()
        setLocalized(Locale.US) {
            val items = MutableStateFlow(PagingData.from(participants(isComputed = true))).collectAsLazyPagingItems()
            MatchBetList(
                lazyPoolGamblerBets = items,
                viewerAccountId = MATCH_PREVIEW_VIEWER_ID,
                isComputed = true,
                onRefresh = {},
                onFeedRestart = {},
                header = { MatchHeader(match = historyBetPreviewModels()[1], presentation = MatchBetsPresentation.COMPUTED) },
                onGamblerOpen = { _, gamblerId, _ -> opened += gamblerId },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composeTestRule.onNodeWithContentDescription("El mono, No bet, 0 points").assertHasClickAction().performClick()
        composeTestRule.onNodeWithContentDescription("felipearpa, You, Bet: España 3, Argentina 2, 0 points")
            .assertHasNoClickAction()
            .performClick()
        opened shouldBe listOf("preview-1")
    }

    @Test
    fun aPlaceholderRowRequestsNoAvatarAndExposesNothing() {
        // Without inspection mode a real avatar would need the avatar store, which is not started.
        setLocalized(Locale.US, inspectionMode = false) {
            MatchGamblerBetItem(
                poolGamblerBet = matchGamblerBetPlaceholderModel(isComputed = true),
                isPlaceholder = true,
                onClick = {},
                modifier = Modifier.fillMaxWidth().testTag(ROW_TAG),
            )
        }

        composeTestRule.onNodeWithTag(ROW_TAG).assertHasNoClickAction().assert(hasNoContentDescription())
        composeTestRule.onAllNodesWithText("X", substring = true).assertCountEquals(0)
    }

    private fun participants(isComputed: Boolean): List<PoolGamblerBetModel> = matchParticipantPreviewModels(isComputed)

    private fun description(tag: String): String =
        composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].single()

    private fun hasNoContentDescription() = SemanticsMatcher("has no content description") { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription).isNullOrEmpty()
    }

    private fun hasNoRole() = SemanticsMatcher("has no role") { node ->
        node.config.getOrNull(SemanticsProperties.Role) == null
    }

    /**
     * Inspection mode makes real participants show the shared letter avatar without the app's
     * avatar store, which these tests do not start. Placeholders render without it, so their test
     * also proves they never reach the store.
     */
    private fun setLocalized(locale: Locale, inspectionMode: Boolean = true, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides inspectionMode) {
                Localized(locale) { TycheTheme { content() } }
            }
        }
    }
}

private val TODAY = LocalDate(2026, 10, 3)
private val SPANISH = Locale.forLanguageTag("es-CO")
private const val ROW_TAG = "matchRow"
private const val HEADER_TAG = "matchHeader"
private const val LOADING_TAG = "ownBetLoading"
