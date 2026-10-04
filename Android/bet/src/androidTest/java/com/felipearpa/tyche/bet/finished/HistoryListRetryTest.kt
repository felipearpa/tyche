package com.felipearpa.tyche.bet.finished

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.bet.timeline.BetTimelineList
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * History and Timeline: when the rows fail while the total is known, the list's Retry requests
 * the rows exactly once more and never the total; a later pull to refresh still reloads both.
 */
class HistoryListRetryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun historyListRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth() {
        val counts = Counts()
        val source = FailingFirstSource()
        composeTestRule.setContent {
            Screen {
                FinishedBetList(
                    lazyPoolGamblerBets = rememberRows(source),
                    pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(589)),
                    onPointsSummaryRetry = { counts.summaryRetries++ },
                    onRefresh = { counts.totalRefreshes++ },
                    modifier = Modifier.fillMaxSize().testTag(LIST_TAG),
                )
            }
        }

        retryThenPull(source = source, counts = counts, total = "589 points earned in this pool")
    }

    @Test
    fun timelineListRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth() {
        val counts = Counts()
        val source = FailingFirstSource()
        composeTestRule.setContent {
            Screen {
                BetTimelineList(
                    // Empty, so the avatar renders the shared letter fallback without a load.
                    gamblerId = "",
                    gamblerUsername = "El mono",
                    lazyBets = rememberRows(source),
                    pointsSummary = HistoryPointsSummaryState.initial.loaded(HistoryPoints.Earned(676)),
                    onPointsSummaryRetry = { counts.summaryRetries++ },
                    onRefresh = { counts.totalRefreshes++ },
                    modifier = Modifier.fillMaxSize().testTag(LIST_TAG),
                )
            }
        }

        retryThenPull(source = source, counts = counts, total = "El mono earned 676 points in this pool")
    }

    private fun retryThenPull(source: FailingFirstSource, counts: Counts, total: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Retry")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(total).assertExists()

        composeTestRule.onNodeWithText("Retry").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { rowShown() }
        settle()
        source.loads.get() shouldBe 2
        counts.summaryRetries shouldBe 0
        counts.totalRefreshes shouldBe 0
        composeTestRule.onNodeWithContentDescription(total).assertExists()

        composeTestRule.onNodeWithTag(LIST_TAG).performTouchInput { swipeDown(durationMillis = 600) }

        composeTestRule.waitUntil(timeoutMillis = 5_000) { source.loads.get() == 3 }
        settle()
        source.loads.get() shouldBe 3
        counts.totalRefreshes shouldBe 1
        counts.summaryRetries shouldBe 0
    }

    private fun rowShown() =
        composeTestRule.onAllNodes(hasContentDescription("2 to 1", substring = true))
            .fetchSemanticsNodes().isNotEmpty()

    /** Lets any further, unexpected load start and finish before counting. */
    private fun settle() {
        composeTestRule.mainClock.advanceTimeBy(LOAD_MILLIS * 4)
        composeTestRule.waitForIdle()
    }
}

@androidx.compose.runtime.Composable
private fun Screen(content: @androidx.compose.runtime.Composable () -> Unit) {
    Localized(Locale.US) { TycheTheme { Surface { content() } } }
}

@androidx.compose.runtime.Composable
private fun rememberRows(source: FailingFirstSource) = remember {
    Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) { source.create() }
}.flow.collectAsLazyPagingItems()

private class Counts {
    var summaryRetries = 0
    var totalRefreshes = 0
}

/** One page of rows; the first load fails. [loads] counts every load. */
private class FailingFirstSource {
    val loads = AtomicInteger(0)
    private val failNext = AtomicBoolean(true)

    fun create(): PagingSource<Int, PoolGamblerBetModel> = object : PagingSource<Int, PoolGamblerBetModel>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PoolGamblerBetModel> {
            loads.incrementAndGet()
            // Like a network page, the load spans frames, so the list composes its loading state.
            delay(LOAD_MILLIS)
            if (failNext.getAndSet(false)) return LoadResult.Error(IOException("offline"))
            return LoadResult.Page(data = historyBetPreviewModels(), prevKey = null, nextKey = null)
        }

        override fun getRefreshKey(state: PagingState<Int, PoolGamblerBetModel>): Int? = null
    }
}

private const val LIST_TAG = "list"
private const val LOAD_MILLIS = 300L
