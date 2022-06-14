package com.pipel.ui

import androidx.paging.PagingSource
import com.pipel.core.CursorPage
import com.pipel.core.PagingQuery
import com.pipel.ui.paging.QueryablePagingSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QueryablePagingSourceTest {

    @ExperimentalCoroutinesApi
    @Test
    fun `given a paging query when function load of QueryablePagingSource is called then a LoadResult Page with data is returned`() =
        runTest {
            val items = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            val nextToken = "10"
            val pagingQuery = mockk<PagingQuery<Int>>()
            coEvery { pagingQuery.execute(any()) } returns
                    CursorPage(
                        items = items,
                        nextToken = nextToken
                    )

            val queryablePagingSource = QueryablePagingSource(pagingQuery = pagingQuery)
            val loadResult = queryablePagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )

            assertTrue(
                loadResult is PagingSource.LoadResult.Page,
                "loadResult isn't a LoadResult.Page"
            )

            val page = loadResult as PagingSource.LoadResult.Page
            assertEquals(
                items,
                page.data,
                "Items aren't equal"
            )
            assertEquals(
                nextToken,
                page.nextKey,
                "Next token aren't equal"
            )
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `given a paging query that raises an exception when function load of QueryablePagingSource is called then a LoadResult Error is returned`() =
        runTest {
            val pagingQuery = mockk<PagingQuery<Int>>()
            coEvery { pagingQuery.execute(any()) } throws RuntimeException()

            val queryablePagingSource = QueryablePagingSource(pagingQuery = pagingQuery)
            val loadResult = queryablePagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )

            assertTrue(
                loadResult is PagingSource.LoadResult.Error,
                "loadResult isn't a LoadResult.Error"
            )
        }
}