package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.creator.PoolLayoutModel
import com.felipearpa.tyche.pool.creator.poolLayoutDummyModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * "My pools" without pools shows popular templates; when they fail to load, the section's Retry
 * requests only the templates, once.
 */
class PoolScoreTemplatesRetryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aFailedTemplatesLoadOffersRetryThatRequestsTheTemplatesOnce() {
        val templates = FailingFirstTemplates()
        composeTestRule.setContent {
            val pager = remember { Pager(PagingConfig(pageSize = 10)) { templates.create() } }
            TycheTheme {
                Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                    PoolScoreList(
                        lazyPoolGamblerScores = MutableStateFlow(noPools()).collectAsLazyPagingItems(),
                        lazyPoolLayouts = pager.flow.collectAsLazyPagingItems(),
                        onPoolOpen = { _, _ -> },
                        onPoolJoin = {},
                        onPoolLayoutSelect = {},
                        onSeeAllTemplates = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Retry")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Retry"))
        composeTestRule.onNodeWithText("Retry").assertHasClickAction().performClick()

        val firstTemplate = poolLayoutDummyModels().first().name
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText(firstTemplate, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()
        assertEquals(2, templates.loads.get())
    }

    private fun noPools(): PagingData<PoolGamblerScoreModel> = PagingData.empty(
        sourceLoadStates = LoadStates(
            refresh = LoadState.NotLoading(endOfPaginationReached = true),
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true),
        ),
    )
}

/** Template pages whose first load fails; [loads] counts every load. */
private class FailingFirstTemplates {
    val loads = AtomicInteger(0)

    fun create(): PagingSource<Int, PoolLayoutModel> = object : PagingSource<Int, PoolLayoutModel>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PoolLayoutModel> =
            if (loads.incrementAndGet() == 1) {
                LoadResult.Error(IOException("offline"))
            } else {
                LoadResult.Page(data = poolLayoutDummyModels(), prevKey = null, nextKey = null)
            }

        override fun getRefreshKey(state: PagingState<Int, PoolLayoutModel>): Int? = null
    }
}
