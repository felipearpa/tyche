package com.felipearpa.tyche.pool.poolscore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.pool.PoolGamblerScoreModel
import com.felipearpa.tyche.pool.creator.PoolFromLayoutCreatorFakeItem
import com.felipearpa.tyche.pool.creator.PoolLayoutModel
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * "My pools" receives its scaffold insets as content padding: the last pool can be scrolled
 * clear of the bottom inset, and a failed first load that does not fit the window (large
 * text in a short window) can still be scrolled to its Retry action, which recovers the list.
 */
class PoolScoreListInsetsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theLastPoolEndsAboveTheBottomPadding() {
        val pools = List(20) { model(index = it) }
        render(pools = MutableStateFlow(PagingData.from(pools)))

        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(pools.lastIndex)

        val lastPoolBottom = composeTestRule
            .onNodeWithContentDescription(pools.last().poolName, substring = true)
            .getUnclippedBoundsInRoot()
            .bottom
        // The row's divider sits between the described row and the end of the item.
        val paddedEnd = VIEWPORT_HEIGHT - BOTTOM_PADDING
        assertTrue(
            "Last pool ends at $lastPoolBottom; the padded end is $paddedEnd",
            lastPoolBottom <= paddedEnd && lastPoolBottom >= paddedEnd - 3.dp,
        )
    }

    @Test
    fun aFailedFirstLoadScrollsToItsRetryActionAndRetryingRecoversTheList() {
        val source = FailingOnceSource(pools = listOf(model(index = 0)))
        render(pools = Pager(PagingConfig(pageSize = 10)) { source }.flow)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
        }
        val retry = composeTestRule.onNodeWithText("Retry").performScrollTo().assertIsDisplayed()
        val retryBottom = retry.getUnclippedBoundsInRoot().bottom
        assertTrue(
            "Retry ends at $retryBottom, inside the bottom padding",
            retryBottom <= VIEWPORT_HEIGHT - BOTTOM_PADDING + 1.dp,
        )

        retry.performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText("Retry")
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Pool 0", substring = true)
            .assertIsDisplayed()
        assertEquals(2, source.loads)
    }

    @Test
    fun templatePlaceholderExposesNoValuesOrActions() {
        composeTestRule.setContent {
            TycheTheme {
                PoolFromLayoutCreatorFakeItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PLACEHOLDER_TAG),
                )
            }
        }

        composeTestRule.onAllNodesWithText("X", substring = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Starting", substring = true).assertCountEquals(0)
        composeTestRule.onNodeWithTag(PLACEHOLDER_TAG).assertHasNoClickAction()
    }

    private fun render(pools: Flow<PagingData<PoolGamblerScoreModel>>) {
        val layouts = MutableStateFlow(PagingData.empty<PoolLayoutModel>())
        composeTestRule.setContent {
            TycheTheme {
                // Short enough that the error message and its Retry action do not fit together.
                Box(modifier = Modifier.size(width = 320.dp, height = VIEWPORT_HEIGHT)) {
                    PoolScoreList(
                        lazyPoolGamblerScores = pools.collectAsLazyPagingItems(),
                        lazyPoolLayouts = layouts.collectAsLazyPagingItems(),
                        contentPadding = PaddingValues(top = TOP_PADDING, bottom = BOTTOM_PADDING),
                        onPoolOpen = { _, _ -> },
                        onPoolJoin = {},
                        onPoolLayoutSelect = {},
                        onSeeAllTemplates = {},
                        fakeItemCount = 3,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private class FailingOnceSource(
        private val pools: List<PoolGamblerScoreModel>,
    ) : PagingSource<Int, PoolGamblerScoreModel>() {
        var loads = 0
            private set

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PoolGamblerScoreModel> {
            loads += 1
            return if (loads == 1) {
                LoadResult.Error(IOException("offline"))
            } else {
                LoadResult.Page(data = pools, prevKey = null, nextKey = null)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, PoolGamblerScoreModel>): Int? = null
    }

    private fun model(index: Int) = PoolGamblerScoreModel(
        poolId = "pool$index",
        poolName = "Pool $index",
        gamblerId = "gambler",
        gamblerUsername = "gambler",
        position = 1,
        beforePosition = 1,
        score = 10,
        gamblerCount = 2,
    )
}

private const val PLACEHOLDER_TAG = "templatePlaceholder"
private val VIEWPORT_HEIGHT = 280.dp
private val TOP_PADDING = 64.dp
private val BOTTOM_PADDING = 48.dp
