package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.bet.kickoffgroup.KickoffGroupModel
import com.felipearpa.tyche.core.type.RankMovement
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * The grouped History list as the signed-in app showed it against production data: each kickoff
 * header is one non-actionable heading that announces its time, match count, and movement; each
 * of a group's rows opens its own match; and the last group scrolls clear of the bottom padding
 * the pool screen passes for its tab bar and the navigation bar.
 */
class HistoryGroupedListTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aTwoMatchHeaderIsOneHeadingWithItsCountAndMovementAndEachRowOpensItsOwnMatch() {
        val opened = mutableListOf<String>()
        val group = group(index = 0, matchCount = 2, places = 0)
        render(groups = listOf(group), onMatchOpen = { opened += it.matchId })

        val header = hasContentDescription(". 2 matches. Rank unchanged", substring = true)
        waitFor(header)
        composeTestRule.onAllNodes(header).assertCountEquals(1)
        composeTestRule.onNode(header and isHeading() and hasNoClickAction()).assertExists()

        group.matches.forEach { match ->
            composeTestRule.onNode(hasContentDescription(match.homeTeamName, substring = true))
                .assertHasClickAction()
                .performClick()
        }

        opened shouldContainExactly group.matches.map { it.matchId }
    }

    @Test
    fun theLastGroupEndsAboveTheBottomPaddingAtTheScrollLimit() {
        val groups = List(GROUP_COUNT) { index -> group(index = index, matchCount = 1 + index % 2, places = index % 3 - 1) }
        val lastRow = hasContentDescription(groups.last().matches.last().homeTeamName, substring = true)
        render(groups = groups, onMatchOpen = {})
        waitFor(hasContentDescription("matches. ", substring = true))

        val list = composeTestRule.onNode(hasScrollToIndexAction())
        list.performScrollToNode(lastRow)
        // Fling past the end so the list rests at its scroll limit.
        repeat(3) { list.performTouchInput { swipeUp() } }
        composeTestRule.waitForIdle()

        val lastRowBottom = composeTestRule.onNode(lastRow).getUnclippedBoundsInRoot().bottom
        val paddedEnd = VIEWPORT_HEIGHT - BOTTOM_PADDING
        // The section's own bottom padding sits between its last row and the end of the item.
        assertTrue(
            "Last row ends at $lastRowBottom; the padded end is $paddedEnd",
            lastRowBottom <= paddedEnd && lastRowBottom >= paddedEnd - SECTION_BOTTOM_SPACE,
        )
    }

    private fun render(groups: List<KickoffGroupModel>, onMatchOpen: (PoolGamblerBetModel) -> Unit) {
        composeTestRule.setContent {
            Localized(Locale.US) {
                TycheTheme {
                    Surface {
                        Box(modifier = Modifier.size(width = 360.dp, height = VIEWPORT_HEIGHT)) {
                            FinishedBetList(
                                lazyKickoffGroups = MutableStateFlow(PagingData.from(groups)).collectAsLazyPagingItems(),
                                pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(589)),
                                onPointsSummaryRetry = {},
                                onRefresh = {},
                                onFeedRestart = {},
                                contentPadding = PaddingValues(top = TOP_PADDING, bottom = BOTTOM_PADDING),
                                onMatchOpen = onMatchOpen,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    /** Paging delivers the first page asynchronously, so wait for it before asserting. */
    private fun waitFor(matcher: SemanticsMatcher) {
        composeTestRule.waitUntil(timeoutMillis = LOAD_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** A group of [matchCount] settled matches, a day apart from its neighbors, newest first. */
    private fun group(index: Int, matchCount: Int, places: Int): KickoffGroupModel {
        val base = historyBetPreviewModels().first()
        return KickoffGroupModel(
            kickoffDateTime = FIRST_KICKOFF - (index * 24).hours,
            totalMatches = matchCount,
            computedMatches = matchCount,
            movement = RankMovement.Moved(places = places, partialProgress = null),
            matches = List(matchCount) { position ->
                base.copy(matchId = "match-$index-$position", homeTeamName = "Home $index-$position")
            },
        )
    }

    private fun isHeading() = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    private fun hasNoClickAction() = SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick)
}

private const val GROUP_COUNT = 12
private const val LOAD_TIMEOUT_MILLIS = 5_000L
private val FIRST_KICKOFF = Instant.parse("2026-07-19T19:00:00Z")
private val VIEWPORT_HEIGHT = 480.dp
private val TOP_PADDING = 64.dp
private val BOTTOM_PADDING = 80.dp
private val SECTION_BOTTOM_SPACE = 20.dp
