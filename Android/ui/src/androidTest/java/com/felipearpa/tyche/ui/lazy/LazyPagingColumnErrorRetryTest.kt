package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * The shared full-list error offers a Retry button, and through `RefreshableLazyPagingColumn`'s
 * default it requests the list exactly once more; pull to refresh keeps working beside it.
 */
class LazyPagingColumnErrorRetryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theErrorShowsALabelledRetryButtonThatRunsItsActionOnce() {
        var retries = 0
        composeTestRule.setContent {
            TycheTheme {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.size(320.dp, 480.dp)) {
                    lazyPagingColumnError(exception = IOException("offline"), onRetry = { retries++ })
                }
            }
        }

        composeTestRule.onNodeWithText("Retry")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        retries shouldBe 1
    }

    @Test
    fun retryAfterAFailedFirstLoadRequestsTheListOnceAndPullToRefreshStillReloads() {
        val source = ScriptedSource(failRefresh = true)
        renderList(source)
        waitForRetry()

        source.failRefresh.set(false)
        composeTestRule.onNodeWithText("Retry").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { rowShown() }
        settle()
        source.refreshLoads.get() shouldBe 2

        pull()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { source.refreshLoads.get() == 3 }
        settle()
        source.refreshLoads.get() shouldBe 3
    }

    @Test
    fun retryAfterAFailedPullOverLoadedRowsReloadsTheList() {
        val source = ScriptedSource(failRefresh = false)
        renderList(source)
        composeTestRule.waitUntil(timeoutMillis = 5_000) { rowShown() }

        source.failRefresh.set(true)
        pull()
        waitForRetry()
        val loadsBeforeRetry = source.refreshLoads.get()

        source.failRefresh.set(false)
        composeTestRule.onNodeWithText("Retry").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { rowShown() }
        settle()
        source.refreshLoads.get() shouldBe loadsBeforeRetry + 1
    }

    private fun waitForRetry() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Retry")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun rowShown() = composeTestRule.onAllNodes(hasText(ROW_TEXT)).fetchSemanticsNodes().isNotEmpty()

    /** Lets any further, unexpected load start and finish before counting. */
    private fun settle() {
        composeTestRule.mainClock.advanceTimeBy(LOAD_MILLIS * 4)
        composeTestRule.waitForIdle()
    }

    private fun pull() {
        composeTestRule.onNodeWithTag(LIST_TAG).performTouchInput { swipeDown(durationMillis = 600) }
    }

    private fun renderList(source: ScriptedSource) {
        composeTestRule.setContent {
            val pager = remember {
                Pager(PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) { source.create() }
            }
            val lazyItems = pager.flow.collectAsLazyPagingItems()
            TycheTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = 480.dp)) {
                    RefreshableLazyPagingColumn(
                        lazyPagingItems = lazyItems,
                        modifier = Modifier.fillMaxSize().testTag(LIST_TAG),
                    ) {
                        items(count = lazyItems.itemCount) { index ->
                            lazyItems[index]
                            Text(text = ROW_TEXT, modifier = Modifier.fillMaxWidth().height(56.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pages of [PAGE_SIZE] rows without end, so visible rows keep asking for the next page. First
 * loads fail while [failRefresh] is set; [refreshLoads] counts first loads only.
 */
private class ScriptedSource(failRefresh: Boolean) {
    val failRefresh = AtomicBoolean(failRefresh)
    val refreshLoads = AtomicInteger(0)

    fun create(): PagingSource<Int, Int> = object : PagingSource<Int, Int>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Int> {
            val page = params.key ?: 0
            if (params.key == null) refreshLoads.incrementAndGet()
            // Like a network page, the load spans frames, so the list composes its loading state.
            delay(LOAD_MILLIS)
            if (params.key == null && failRefresh.get()) return LoadResult.Error(IOException("offline"))
            return LoadResult.Page(
                data = List(PAGE_SIZE) { page * PAGE_SIZE + it },
                prevKey = null,
                nextKey = page + 1,
            )
        }

        override fun getRefreshKey(state: PagingState<Int, Int>): Int? = null
    }
}

private const val LIST_TAG = "list"
private const val ROW_TEXT = "row"
private const val LOAD_MILLIS = 300L
private const val PAGE_SIZE = 20
