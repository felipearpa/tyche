package com.felipearpa.tyche.ui.lazy

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.compose.LazyPagingItems
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class StatefulLazyColumnTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenALoadingPagingItemWhenInViewThenTheLoadingContentIsShown() {
        var isLoadingContentExecuted = false

        composeTestRule.setContent {
            val lazyItems = givenALoadingItem()

            StatefulLazyColumn(
                lazyItems = lazyItems,
                loadingContent = { isLoadingContentExecuted = true }
            ) {}
        }

        isLoadingContentExecuted.shouldBeTrue()
    }

    @Test
    fun givenAnAppendLoadingPagingItemWhenInViewThenTheLoadingContentOnConcatenateIsShown() {
        var isLoadingContentOnConcatenateExecuted = false

        composeTestRule.setContent {
            val lazyItems = givenAnAppendLoadingItem()

            StatefulLazyColumn(
                lazyItems = lazyItems,
                loadingContentOnConcatenate = { isLoadingContentOnConcatenateExecuted = true }
            ) {}
        }

        isLoadingContentOnConcatenateExecuted.shouldBeTrue()
    }

    @Test
    fun givenAPrependLoadingPagingItemWhenInViewThenTheLoadingContentOnConcatenateIsShown() {
        var isLoadingContentOnConcatenateExecuted = false

        composeTestRule.setContent {
            val lazyItems = givenAnPrependLoadingItem()

            StatefulLazyColumn(
                lazyItems = lazyItems,
                loadingContentOnConcatenate = { isLoadingContentOnConcatenateExecuted = true }
            ) {}
        }

        isLoadingContentOnConcatenateExecuted.shouldBeTrue()
    }

    @Test
    fun givenAnErrorPagingItemWhenInViewThenTheErrorContentIsShown() {
        var isErrorContentExecuted = false

        composeTestRule.setContent {
            val lazyItems = givenAnErrorItem()

            StatefulLazyColumn(
                lazyItems = lazyItems,
                errorContent = { isErrorContentExecuted = true }
            ) {}
        }

        isErrorContentExecuted.shouldBeTrue()
    }

    @Test
    fun givenAnAppendErrorPagingItemWhenInViewThenTheErrorContentOnConcatenateIsShown() {
        var isErrorContentOnConcatenateExecuted = false

        composeTestRule.setContent {
            val lazyItems = givenAnAppendErrorItem()

            StatefulLazyColumn(
                lazyItems = lazyItems,
                errorContentOnConcatenate = { isErrorContentOnConcatenateExecuted = true }
            ) {}
        }

        isErrorContentOnConcatenateExecuted.shouldBeTrue()
    }

    @Test
    fun givenAPrependErrorPagingItemWhenInViewThenTheErrorContentOnConcatenateIsShown() {
        var isErrorContentOnConcatenateExecuted = false

        composeTestRule.setContent {
            val lazyItems = givenAPrependErrorPagingItem()

            StatefulLazyColumn(
                lazyItems = lazyItems,
                errorContentOnConcatenate = { isErrorContentOnConcatenateExecuted = true }
            ) {}
        }

        isErrorContentOnConcatenateExecuted.shouldBeTrue()
    }

    @Test
    fun givenAnEmptyPagingItemWhenInViewThenTheEmptyContentIsShown() {
        var isEmptyContentExecuted = false

        composeTestRule.setContent {
            val lazyItems = givenAnEmptyPagingItem()

            StatefulLazyColumn(
                lazyItems = lazyItems,
                emptyContent = { isEmptyContentExecuted = true }
            ) {}
        }

        isEmptyContentExecuted.shouldBeTrue()
    }

    @Test
    fun givenAFilledPagingItemWhenInViewThenTheItemContentIsShown() {
        var isItemContentExecuted = false

        composeTestRule.setContent {
            val lazyItems = givenAFilledPagingItem()

            StatefulLazyColumn(lazyItems = lazyItems) {
                isItemContentExecuted = true
            }
        }

        isItemContentExecuted.shouldBeTrue()
    }

    private fun givenALoadingItem(): LazyPagingItems<Any> {
        return mockk<LazyPagingItems<Any>>().also { lazyItems ->
            every { lazyItems.itemCount } returns 0
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

        }
    }

    private fun givenAnAppendLoadingItem(): LazyPagingItems<Any> {
        return mockk<LazyPagingItems<Any>>().also { lazyItems ->
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
        }
    }

    private fun givenAnPrependLoadingItem(): LazyPagingItems<Any> {
        return mockk<LazyPagingItems<Any>>().also { lazyItems ->
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
        }
    }

    private fun givenAnErrorItem(): LazyPagingItems<Any> {
        return mockk<LazyPagingItems<Any>>().also { lazyItems ->
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
        }
    }

    private fun givenAnAppendErrorItem(): LazyPagingItems<Any> {
        return mockk<LazyPagingItems<Any>>().also { lazyItems ->
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
        }
    }

    private fun givenAPrependErrorPagingItem(): LazyPagingItems<Any> {
        return mockk<LazyPagingItems<Any>>().also { lazyItems ->
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
        }
    }

    private fun givenAnEmptyPagingItem(): LazyPagingItems<Any> {
        return mockk<LazyPagingItems<Any>>().also { lazyItems ->
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
        }
    }

    private fun givenAFilledPagingItem(): LazyPagingItems<Any> {
        return mockk<LazyPagingItems<Any>>().also { lazyItems ->
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
        }
    }
}