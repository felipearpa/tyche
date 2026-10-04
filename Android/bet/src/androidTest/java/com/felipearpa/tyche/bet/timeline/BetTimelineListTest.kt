package com.felipearpa.tyche.bet.timeline

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.HistoryPoints
import com.felipearpa.tyche.bet.finished.HistoryPointsSummaryState
import com.felipearpa.tyche.bet.finished.Localized
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Timeline's composition: the selected gambler's identity and total stay usable whatever the
 * rows do, real rows open their match once, and placeholder rows expose nothing.
 */
class BetTimelineListTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun identityIsAHeadingAndTheTotalIsAnnouncedForTheSelectedGambler() {
        setTimeline(rows = PagingData.from(listOf(settledRow())))

        composeTestRule.onNode(hasHeading("El mono")).assertExists()
        composeTestRule.onNodeWithContentDescription("El mono earned 676 points in this pool").assertExists()
    }

    @Test
    fun aRowOpensItsMatchOnceAndTheHeaderOpensNothing() {
        val opened = mutableListOf<String>()
        setTimeline(rows = PagingData.from(listOf(settledRow())), onMatchOpen = { opened += it.matchId })

        composeTestRule.onNode(hasHeading("El mono")).assertHasNoAction()
        composeTestRule.onNodeWithContentDescription("El mono earned 676 points in this pool").assertHasNoAction()
        composeTestRule.onNode(hasContentDescription("Bet: 2 to 1", substring = true))
            .assertHasClickAction()
            .performClick()

        opened shouldContainExactly listOf("history-positive")
    }

    @Test
    fun whenRowsFailTheIdentityAndTotalStayAboveTheFailure() {
        val failed = LoadStates(
            refresh = LoadState.Error(UnknownLocalizedException()),
            prepend = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false),
        )
        setTimeline(rows = PagingData.empty(sourceLoadStates = failed))

        composeTestRule.onNode(hasHeading("El mono")).assertExists()
        composeTestRule.onNodeWithContentDescription("El mono earned 676 points in this pool").assertExists()
        // The shared list failure, with its Retry beside pull to refresh.
        composeTestRule.onNodeWithText("Unexpected error").assertExists()
        composeTestRule.onNodeWithText("Retry").assertHasClickAction()
    }

    @Test
    fun whenTheTotalFailsRowsStayUsableAndRetryAsksOnlyForTheTotal() {
        var summaryRetries = 0
        val opened = mutableListOf<String>()
        setTimeline(
            rows = PagingData.from(listOf(settledRow())),
            pointsSummary = HistoryPointsSummaryState.initial.failed(),
            onPointsSummaryRetry = { summaryRetries++ },
            onMatchOpen = { opened += it.matchId },
        )

        composeTestRule.onNodeWithText("Couldn't load points.").assertExists()
        composeTestRule.onNodeWithContentDescription("Retry").performClick()
        composeTestRule.onNode(hasContentDescription("Bet: 2 to 1", substring = true)).performClick()

        summaryRetries shouldBe 1
        opened shouldContainExactly listOf("history-positive")
    }

    @Test
    fun whileRowsAndTotalLoadTheIdentityIsRealAndPlaceholdersAreInert() {
        val loading = LoadStates(
            refresh = LoadState.Loading,
            prepend = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false),
        )
        setTimeline(
            rows = PagingData.empty(sourceLoadStates = loading),
            pointsSummary = HistoryPointsSummaryState.initial.requesting(HistoryPointsSummaryState.Request.LOADING),
            placeholderCount = 3,
        )

        composeTestRule.onNode(hasHeading("El mono")).assertExists()
        composeTestRule.onAllNodesWithText("X", substring = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("pt", substring = true).assertCountEquals(0)
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        composeTestRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .assertCountEquals(0)
    }

    private fun setTimeline(
        rows: PagingData<PoolGamblerBetModel>,
        pointsSummary: HistoryPointsSummaryState =
            HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(676)),
        onPointsSummaryRetry: () -> Unit = {},
        placeholderCount: Int = 0,
        onMatchOpen: (PoolGamblerBetModel) -> Unit = {},
    ) {
        composeTestRule.setContent {
            Localized(Locale.US) {
                TycheTheme {
                    Surface {
                        BetTimelineList(
                            // Empty, so the avatar renders the shared letter fallback without a load.
                            gamblerId = "",
                            gamblerUsername = "El mono",
                            lazyBets = MutableStateFlow(rows).collectAsLazyPagingItems(),
                            pointsSummary = pointsSummary,
                            onPointsSummaryRetry = onPointsSummaryRetry,
                            onRefresh = {},
                            placeholderCount = placeholderCount,
                            onMatchOpen = onMatchOpen,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    private fun settledRow() = historyBetPreviewModels().first()

    private fun hasHeading(text: String) =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading) and
            androidx.compose.ui.test.hasText(text)

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertHasNoAction() =
        assert(SemanticsMatcher.keyNotDefined(androidx.compose.ui.semantics.SemanticsActions.OnClick))
}
