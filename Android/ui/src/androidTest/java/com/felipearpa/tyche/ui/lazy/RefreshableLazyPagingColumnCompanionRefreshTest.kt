package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

/**
 * One pull refreshes the list and starts the caller's companion refresh; the pull indicator
 * stays while that companion work is pending. Callers that pass neither keep a list-only refresh.
 */
class RefreshableLazyPagingColumnCompanionRefreshTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onePullRefreshesTheListAndStartsTheCompanionRefreshOnce() {
        val listLoads = AtomicInteger(0)
        var companionRefreshes = 0
        renderList(listLoads = listLoads, onRefresh = { companionRefreshes++ })
        composeTestRule.waitUntil { listLoads.get() == 1 }

        pull()

        composeTestRule.waitUntil { listLoads.get() == 2 }
        companionRefreshes shouldBe 1
    }

    @Test
    fun theIndicatorStaysUntilTheCompanionRefreshFinishesAfterTheList() {
        val listLoads = AtomicInteger(0)
        var isCompanionRefreshing by mutableStateOf(false)
        renderList(
            listLoads = listLoads,
            onRefresh = { isCompanionRefreshing = true },
            isCompanionRefreshing = { isCompanionRefreshing },
        )
        composeTestRule.waitUntil { listLoads.get() == 1 }

        pull()
        composeTestRule.waitUntil { listLoads.get() == 2 }
        composeTestRule.mainClock.advanceTimeBy(LOAD_MILLIS * 2)
        composeTestRule.waitForIdle()
        refreshIndicatorCount() shouldBe 1

        isCompanionRefreshing = false
        composeTestRule.waitUntil(timeoutMillis = 5_000) { refreshIndicatorCount() == 0 }
    }

    @Test
    fun withoutACompanionThePullRefreshesOnlyTheListAndTheIndicatorEndsWithIt() {
        val listLoads = AtomicInteger(0)
        renderList(listLoads = listLoads)
        composeTestRule.waitUntil { listLoads.get() == 1 }

        pull()

        composeTestRule.waitUntil { listLoads.get() == 2 }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { refreshIndicatorCount() == 0 }
    }

    private fun refreshIndicatorCount() =
        composeTestRule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .fetchSemanticsNodes().size

    private fun pull() {
        composeTestRule.onNodeWithTag(LIST_TAG).performTouchInput { swipeDown(durationMillis = 600) }
    }

    private fun renderList(
        listLoads: AtomicInteger,
        onRefresh: (() -> Unit)? = null,
        isCompanionRefreshing: () -> Boolean = { false },
    ) {
        composeTestRule.setContent {
            val pager = remember {
                Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) { CountingPagingSource(listLoads) }
            }
            val lazyItems = pager.flow.collectAsLazyPagingItems()
            TycheTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = 480.dp)) {
                    if (onRefresh == null) {
                        RefreshableLazyPagingColumn(
                            lazyPagingItems = lazyItems,
                            modifier = Modifier.fillMaxSize().testTag(LIST_TAG),
                        ) {
                            items(count = lazyItems.itemCount) { Box(Modifier.fillMaxWidth().height(56.dp)) }
                        }
                    } else {
                        RefreshableLazyPagingColumn(
                            lazyPagingItems = lazyItems,
                            onRefresh = onRefresh,
                            isCompanionRefreshing = isCompanionRefreshing(),
                            modifier = Modifier.fillMaxSize().testTag(LIST_TAG),
                        ) {
                            items(count = lazyItems.itemCount) { Box(Modifier.fillMaxWidth().height(56.dp)) }
                        }
                    }
                }
            }
        }
    }
}

private class CountingPagingSource(private val loads: AtomicInteger) : PagingSource<Int, Int>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
        loads.incrementAndGet()
        // Like a network page, the load spans frames, so the list composes its loading state.
        delay(LOAD_MILLIS)
        return LoadResult.Page(data = List(30) { it }, prevKey = null, nextKey = null)
    }

    override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
}

private const val LIST_TAG = "list"
private const val LOAD_MILLIS = 300L
