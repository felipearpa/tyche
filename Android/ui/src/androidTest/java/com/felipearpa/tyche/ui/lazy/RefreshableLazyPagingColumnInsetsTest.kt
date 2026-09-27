package com.felipearpa.tyche.ui.lazy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.felipearpa.tyche.ui.theme.TycheTheme
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Screens pass their scaffold insets to the shared paging list as content padding. The
 * list's viewport keeps the full height it is given, so rows can scroll under app bars and
 * system bars, while the padding still lets the first and last rows come fully clear of
 * them. Empty and error states fill the padded area and scroll when they do not fit, so a
 * recovery action is never clipped.
 */
class RefreshableLazyPagingColumnInsetsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theViewportKeepsItsFullHeightAndTheFirstRowStartsBelowTheTopPadding() {
        renderList(loadedItems())

        composeTestRule.onNode(hasScrollToIndexAction()).assertHeightIs(VIEWPORT_HEIGHT)
        composeTestRule.onNodeWithTag(rowTag(0)).assertTopIs(TOP_PADDING)
    }

    @Test
    fun theLastRowEndsAboveTheBottomPaddingAtTheEndOfTheList() {
        renderList(loadedItems())

        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToIndex(ROW_COUNT - 1)

        composeTestRule.onNodeWithTag(rowTag(ROW_COUNT - 1)).assertBottomIs(VIEWPORT_HEIGHT - BOTTOM_PADDING)
    }

    @Test
    fun anEmptyStateIsCenteredInThePaddedArea() {
        renderList(
            items = MutableStateFlow(
                PagingData.empty(
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.NotLoading(endOfPaginationReached = true),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true),
                    ),
                ),
            ),
            emptyContent = {
                item {
                    ViewportFillingItem {
                        Box(modifier = Modifier.size(40.dp).testTag(EMPTY_TAG))
                    }
                }
            },
        )

        val bounds = composeTestRule.onNodeWithTag(EMPTY_TAG).assertIsDisplayed().getUnclippedBoundsInRoot()
        val paddedCenter = (TOP_PADDING + (VIEWPORT_HEIGHT - BOTTOM_PADDING)) / 2
        ((bounds.top + bounds.bottom) / 2).assertNear(paddedCenter)
    }

    @Test
    fun aTallErrorStateScrollsItsRecoveryActionIntoTheSafeArea() {
        renderList(
            items = MutableStateFlow(
                PagingData.empty(
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.Error(IllegalStateException("offline")),
                        prepend = LoadState.NotLoading(endOfPaginationReached = false),
                        append = LoadState.NotLoading(endOfPaginationReached = false),
                    ),
                ),
            ),
            errorContent = {
                item {
                    ViewportFillingItem {
                        // Taller than the padded viewport, as with large text in a short window.
                        Box(modifier = Modifier.fillMaxWidth().height(VIEWPORT_HEIGHT))
                        Button(onClick = {}, modifier = Modifier.testTag(RETRY_TAG)) {
                            Text(text = "Retry")
                        }
                    }
                }
            },
        )

        composeTestRule.onNodeWithTag(RETRY_TAG).performScrollTo().assertIsDisplayed()
        val retryBounds = composeTestRule.onNodeWithTag(RETRY_TAG).getUnclippedBoundsInRoot()
        retryBounds.bottom.value shouldBeLessThanOrEqual (VIEWPORT_HEIGHT - BOTTOM_PADDING).value + TOLERANCE
        retryBounds.top.value shouldBeGreaterThanOrEqual TOP_PADDING.value - TOLERANCE
    }

    private fun loadedItems() = MutableStateFlow(PagingData.from(List(ROW_COUNT) { it }))

    private fun renderList(
        items: MutableStateFlow<PagingData<Int>>,
        errorContent: LazyListScope.(Throwable) -> Unit = { lazyPagingColumnError(it) },
        emptyContent: LazyListScope.() -> Unit = { lazyPagingColumnEmpty() },
    ) {
        composeTestRule.setContent {
            TycheTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = VIEWPORT_HEIGHT)) {
                    val lazyItems = items.collectAsLazyPagingItems()
                    RefreshableLazyPagingColumn(
                        lazyPagingItems = lazyItems,
                        contentPadding = PaddingValues(top = TOP_PADDING, bottom = BOTTOM_PADDING),
                        errorContent = errorContent,
                        emptyContent = emptyContent,
                        modifier = Modifier.fillMaxSize(),
                    ) { rows(lazyItems) }
                }
            }
        }
    }

    private fun LazyListScope.rows(lazyItems: LazyPagingItems<Int>) {
        items(count = lazyItems.itemCount) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .testTag(rowTag(index)),
            )
        }
    }

    private fun SemanticsNodeInteraction.assertHeightIs(expected: Dp) {
        val bounds = getUnclippedBoundsInRoot()
        (bounds.bottom - bounds.top).assertNear(expected)
    }

    private fun SemanticsNodeInteraction.assertTopIs(expected: Dp) {
        getUnclippedBoundsInRoot().top.assertNear(expected)
    }

    private fun SemanticsNodeInteraction.assertBottomIs(expected: Dp) {
        getUnclippedBoundsInRoot().bottom.assertNear(expected)
    }

    private fun Dp.assertNear(expected: Dp) {
        value shouldBe (expected.value plusOrMinus TOLERANCE)
    }

    private fun rowTag(index: Int) = "row:$index"
}

private const val ROW_COUNT = 30
private const val RETRY_TAG = "retry"
private const val EMPTY_TAG = "empty"
private val VIEWPORT_HEIGHT = 480.dp
private val TOP_PADDING = 64.dp
private val BOTTOM_PADDING = 48.dp
private val ROW_HEIGHT = 56.dp
private const val TOLERANCE = 1f
