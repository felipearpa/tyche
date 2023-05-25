package com.felipearpa.ui.lazy

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.compose.LazyPagingItems
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LazyColumnTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun given_a_lazy_paging_item_in_loading_state_when_LazyColumn_is_displayed_then_loadingContent_is_displayed() {
        var isLoadingContentExecuted = false

        composeTestRule.setContent {
            val lazyItems = mockk<LazyPagingItems<Any>>()

            every { lazyItems.itemCount } returns 10
            every { lazyItems.loadState } returns CombinedLoadStates(
                refresh = LoadState.Loading,
                source = LoadStates(
                    refresh = LoadState.Loading,
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true)
                ),
                append = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true)
            )

            LazyColumn(
                lazyItems = lazyItems,
                loadingContent = { isLoadingContentExecuted = true }
            ) {}
        }

        assertTrue(isLoadingContentExecuted)
    }

    @Test
    fun given_a_lazy_paging_item_in_loading_append_state_when_LazyColumn_is_displayed_then_loadingContentOnConcatenate_is_displayed() {
        var isLoadingContentOnConcatenateExecuted = false

        composeTestRule.setContent {
            val lazyItems = mockk<LazyPagingItems<Any>>()

            every { lazyItems.itemCount } returns 10
            every { lazyItems.loadState } returns CombinedLoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                source = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.Loading,
                    prepend = LoadState.NotLoading(endOfPaginationReached = true)
                ),
                append = LoadState.Loading,
                prepend = LoadState.NotLoading(endOfPaginationReached = true)
            )

            LazyColumn(
                lazyItems = lazyItems,
                loadingContentOnConcatenate = { isLoadingContentOnConcatenateExecuted = true }
            ) {}
        }

        assertTrue(isLoadingContentOnConcatenateExecuted)
    }

    @Test
    fun given_a_lazy_paging_item_in_loading_prepend_state_when_LazyColumn_is_displayed_then_loadingContentOnConcatenate_is_displayed() {
        var isLoadingContentOnConcatenateExecuted = false

        composeTestRule.setContent {
            val lazyItems = mockk<LazyPagingItems<Any>>()

            every { lazyItems.itemCount } returns 10
            every { lazyItems.loadState } returns CombinedLoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                source = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                    prepend = LoadState.Loading
                ),
                append = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.Loading
            )

            LazyColumn(
                lazyItems = lazyItems,
                loadingContentOnConcatenate = { isLoadingContentOnConcatenateExecuted = true }
            ) {}
        }

        assertTrue(isLoadingContentOnConcatenateExecuted)
    }

    @Test
    fun given_a_lazy_paging_item_in_error_state_when_LazyColumn_is_displayed_then_errorContent_is_displayed() {
        var isErrorContentExecuted = false

        composeTestRule.setContent {
            val lazyItems = mockk<LazyPagingItems<Any>>()

            every { lazyItems.itemCount } returns 10
            every { lazyItems.loadState } returns CombinedLoadStates(
                refresh = LoadState.Error(RuntimeException()),
                source = LoadStates(
                    refresh = LoadState.Error(RuntimeException()),
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true)
                ),
                append = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true)
            )

            LazyColumn(
                lazyItems = lazyItems,
                errorContent = { isErrorContentExecuted = true }
            ) {}
        }

        assertTrue(isErrorContentExecuted)
    }

    @Test
    fun given_a_lazy_paging_item_in_error_append_state_when_LazyColumn_is_displayed_then_errorContentOnConcatenate_is_displayed() {
        var isErrorContentOnConcatenateExecuted = false

        composeTestRule.setContent {
            val lazyItems = mockk<LazyPagingItems<Any>>()

            every { lazyItems.itemCount } returns 10
            every { lazyItems.loadState } returns CombinedLoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                source = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.Error(RuntimeException()),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true)
                ),
                append = LoadState.Error(RuntimeException()),
                prepend = LoadState.NotLoading(endOfPaginationReached = true)
            )

            LazyColumn(
                lazyItems = lazyItems,
                errorContentOnConcatenate = { isErrorContentOnConcatenateExecuted = true }
            ) {}
        }

        assertTrue(isErrorContentOnConcatenateExecuted)
    }

    @Test
    fun given_a_lazy_paging_item_in_error_prepend_state_when_LazyColumn_is_displayed_then_errorContentOnConcatenate_is_displayed() {
        var isErrorContentOnConcatenateExecuted = false

        composeTestRule.setContent {
            val lazyItems = mockk<LazyPagingItems<Any>>()

            every { lazyItems.itemCount } returns 10
            every { lazyItems.loadState } returns CombinedLoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                source = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                    prepend = LoadState.Error(RuntimeException())
                ),
                append = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.Error(RuntimeException())
            )

            LazyColumn(
                lazyItems = lazyItems,
                errorContentOnConcatenate = { isErrorContentOnConcatenateExecuted = true }
            ) {}
        }

        assertTrue(isErrorContentOnConcatenateExecuted)
    }

    @Test
    fun given_a_empty_lazy_paging_item_when_LazyColumn_is_displayed_then_emptyContent_is_displayed() {
        var isEmptyContentExecuted = false

        composeTestRule.setContent {
            val lazyItems = mockk<LazyPagingItems<Any>>()

            every { lazyItems.itemCount } returns 0
            every { lazyItems.loadState } returns CombinedLoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                source = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true)
                ),
                append = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true)
            )

            LazyColumn(
                lazyItems = lazyItems,
                emptyContent = { isEmptyContentExecuted = true }
            ) {}
        }

        assertTrue(isEmptyContentExecuted)
    }

    @Test
    fun given_a_filled_lazy_paging_item_when_LazyColumn_is_displayed_then_itemContent_is_displayed() {
        var isItemContentExecuted = false

        composeTestRule.setContent {
            val lazyItems = mockk<LazyPagingItems<Any>>()

            every { lazyItems.itemCount } returns 10
            every { lazyItems.loadState } returns CombinedLoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                source = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true)
                ),
                append = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true)
            )

            LazyColumn(lazyItems = lazyItems) {
                isItemContentExecuted = true
            }
        }

        assertTrue(isItemContentExecuted)
    }
}