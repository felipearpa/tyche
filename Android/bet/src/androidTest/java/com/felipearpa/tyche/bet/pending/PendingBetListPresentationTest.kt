package com.felipearpa.tyche.bet.pending

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import org.junit.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The filter row's counts and the empty-filter message, against the paging state: while more pages
 * remain the counts carry "+" (read as "at least") and no empty message shows; once the last page
 * has loaded the counts are exact and the message appears. Runs in English.
 */
@OptIn(ExperimentalTime::class)
class PendingBetListPresentationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whileMorePagesRemainCountsCarryAPlusAndTheEmptyMessageWaits() {
        setList(isLastPageLoaded = false)

        composeTestRule.onNode(hasContentDescription("No bet, at least 0 matches")).assertExists()
        composeTestRule.onNode(hasContentDescription("All, at least 1 match")).assertExists()
        composeTestRule.onAllNodesWithText(NO_BET_EMPTY_MESSAGE).fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun onceTheLastPageHasLoadedCountsAreExactAndTheEmptyMessageShows() {
        setList(isLastPageLoaded = true)

        composeTestRule.onNode(hasContentDescription("No bet, 0 matches")).assertExists()
        composeTestRule.onNode(hasContentDescription("All, 1 match")).assertExists()
        composeTestRule.onNodeWithText(NO_BET_EMPTY_MESSAGE).assertExists()
    }

    @Test
    fun aFilterShowsOnlyItsCardsUnderTheirDayHeaders() {
        val listState = PendingBetListState(
            filter = PendingBetFilter.NO_BET,
            counts = PendingBetFilterCounts(all = 2, noBet = 1, placed = 1),
            visibleMatchIds = setOf("open"),
        )
        setList(isLastPageLoaded = true, listState = listState, matches = listOf(placed, open))

        composeTestRule.onNode(hasContentDescription("Colombia versus Portugal", substring = true)).assertExists()
        composeTestRule.onAllNodes(hasContentDescription("Brasil versus Japón", substring = true)).fetchSemanticsNodes().size shouldBe 0
        composeTestRule.onAllNodesWithText("Wednesday").fetchSemanticsNodes().size shouldBe 0
        composeTestRule.onNodeWithText("Tomorrow").assertExists()
    }

    private fun setList(
        isLastPageLoaded: Boolean,
        listState: PendingBetListState = noBetFilteredEmpty,
        matches: List<PoolGamblerBetModel> = listOf(placed),
    ) {
        val notLoading = LoadState.NotLoading(endOfPaginationReached = isLastPageLoaded)
        val loadStates = LoadStates(refresh = LoadState.NotLoading(false), prepend = LoadState.NotLoading(true), append = notLoading)
        val pagingData = MutableStateFlow(PagingData.from(matches, sourceLoadStates = loadStates))
        composeTestRule.setContent {
            TycheTheme {
                PendingBetList(
                    lazyPoolGamblerBets = pagingData.collectAsLazyPagingItems(),
                    listState = listState,
                    now = NOW,
                    onSelectFilter = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private val noBetFilteredEmpty = PendingBetListState(
        filter = PendingBetFilter.NO_BET,
        counts = PendingBetFilterCounts(all = 1, noBet = 0, placed = 1),
        visibleMatchIds = emptySet(),
    )

    private val placed = match(id = "placed", home = "Brasil", away = "Japón", day = 21, betScore = TeamScore(2, 1))
    private val open = match(id = "open", home = "Colombia", away = "Portugal", day = 19, betScore = null)

    private fun match(id: String, home: String, away: String, day: Int, betScore: TeamScore<Int>?) = PoolGamblerBetModel(
        poolId = "pool",
        gamblerId = "gambler",
        gamblerUsername = "gambler",
        matchId = id,
        homeTeamId = "br",
        homeTeamName = home,
        awayTeamId = "jp",
        awayTeamName = away,
        matchScore = null,
        betScore = betScore,
        score = null,
        matchDateTime = LocalDateTime(year = 2026, month = 10, day = day, hour = 14, minute = 0),
        isLocked = false,
        isComputed = false,
    )

    private companion object {
        const val NO_BET_EMPTY_MESSAGE = "Nothing left to bet on right now."

        /** Sunday, 18 October 2026, midday in the Americas: the matches fall tomorrow and Wednesday. */
        val NOW = Instant.parse("2026-10-18T17:00:00Z")
    }
}
