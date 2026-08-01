package com.felipearpa.tyche.pool.gamblerscore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
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
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModel
import com.felipearpa.tyche.pool.poolGamblerScoreDummyModels
import com.felipearpa.tyche.ui.theme.TycheTheme
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Loading placeholders must be the production [GamblerScoreItem] populated with
 * `poolGamblerScorePlaceholderModel()` — never a separately maintained skeleton row —
 * so placeholder geometry and future row changes cannot drift from loaded rows.
 * Initial and append loading both render the shared tagged placeholder row, which is
 * inert: it exposes no values, text, or actions to TalkBack and paints no background
 * of its own, so the container's canvas shows through exactly as on loaded rows.
 */
class GamblerScorePlaceholderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun initialLoadingRendersTheSharedPlaceholderRow() {
        // The paging column shows its loading slot for the initial presentation:
        // no items yet and pagination not ended.
        val pagingData = MutableStateFlow(
            PagingData.empty<PoolGamblerScoreModel>(
                sourceLoadStates = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = false),
                    prepend = LoadState.NotLoading(endOfPaginationReached = false),
                    append = LoadState.NotLoading(endOfPaginationReached = false),
                ),
            ),
        )

        composeTestRule.setContent {
            TycheTheme {
                GamblerScoreList(
                    lazyPoolGamblerScores = pagingData.collectAsLazyPagingItems(),
                    loggedInGamblerId = "nobody",
                    modifier = Modifier.fillMaxSize(),
                    placeholderCount = 3,
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(PLACEHOLDER_ROW_TAG)
                .fetchSemanticsNodes()
                .size == 3
        }
    }

    @Test
    fun appendLoadingRendersTheSameSharedPlaceholderRow() {
        val models = poolGamblerScoreDummyModels().take(2)

        composeTestRule.setContent {
            TycheTheme {
                GamblerScoreList(
                    lazyPoolGamblerScores = pendingSecondPagePager(models)
                        .flow.collectAsLazyPagingItems(),
                    loggedInGamblerId = models.first().gamblerId,
                    modifier = Modifier.fillMaxSize(),
                    placeholderCount = 3,
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag(PLACEHOLDER_ROW_TAG)
                .fetchSemanticsNodes()
                .size == 1
        }
        composeTestRule
            .onNodeWithTag("gamblerScoreRow:${models.first().gamblerId}")
            .assertIsDisplayed()
    }

    private fun pendingSecondPagePager(models: List<PoolGamblerScoreModel>) =
        Pager(PagingConfig(pageSize = models.size, prefetchDistance = models.size)) {
            object : PagingSource<Int, PoolGamblerScoreModel>() {
                override suspend fun load(
                    params: LoadParams<Int>,
                ): LoadResult<Int, PoolGamblerScoreModel> =
                    if (params.key == null) {
                        LoadResult.Page(data = models, prevKey = null, nextKey = 1)
                    } else {
                        awaitCancellation()
                    }

                override fun getRefreshKey(
                    state: PagingState<Int, PoolGamblerScoreModel>,
                ): Int? = null
            }
        }

    @Test
    fun placeholderRowPreservesProductionRowGeometryAndInheritedBackground() {
        composeTestRule.setContent {
            TycheTheme {
                Box(modifier = Modifier.background(CONTAINER_COLOR)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.testTag(LOADED_ROW_TAG)) {
                            GamblerScoreItem(
                                poolGamblerScore = poolGamblerScoreDummyModel(),
                                isCurrentUser = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Box(modifier = Modifier.testTag(PLACEHOLDER_ITEM_TAG)) {
                            GamblerScorePlaceholderItem(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        val loadedRow = composeTestRule.onNodeWithTag(LOADED_ROW_TAG)
        val placeholderRow = composeTestRule.onNodeWithTag(PLACEHOLDER_ITEM_TAG)

        loadedRow.assertHeightIsAtLeast(82.dp)
        placeholderRow.assertHeightIsAtLeast(82.dp)
        assertEquals(
            loadedRow.fetchSemanticsNode().boundsInRoot.height,
            placeholderRow.fetchSemanticsNode().boundsInRoot.height,
            1f,
        )

        // Neither row paints its own canvas: the container's color shows through the
        // row padding of the placeholder exactly as it does on a loaded neutral row.
        assertEquals(
            CONTAINER_COLOR.toArgb(),
            loadedRow.captureToImage().toPixelMap()[2, 2].toArgb(),
        )
        assertEquals(
            CONTAINER_COLOR.toArgb(),
            placeholderRow.captureToImage().toPixelMap()[2, 2].toArgb(),
        )
    }

    @Test
    fun placeholderRowIsInertForTalkBackAndActions() {
        composeTestRule.setContent {
            TycheTheme {
                Box(modifier = Modifier.testTag(PLACEHOLDER_ITEM_TAG)) {
                    GamblerScorePlaceholderItem(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        val placeholderUsername = "X".repeat(25)
        composeTestRule
            .onAllNodesWithText(placeholderUsername, substring = true)
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithContentDescription("Rank", substring = true)
            .assertCountEquals(0)
        composeTestRule
            .onNodeWithTag(PLACEHOLDER_ITEM_TAG)
            .assertHasNoClickAction()
    }
}

private const val PLACEHOLDER_ROW_TAG = "gamblerScorePlaceholderRow"
private const val PLACEHOLDER_ITEM_TAG = "gamblerScorePlaceholderItem"
private const val LOADED_ROW_TAG = "gamblerScoreLoadedRow"
private val CONTAINER_COLOR = Color(0xFF123456)
