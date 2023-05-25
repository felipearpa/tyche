package com.felipearpa.ui.paging

import androidx.paging.PagingSource
import com.felipearpa.core.paging.CursorPage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryablePagingSourceTest {

    @ExperimentalCoroutinesApi
    @Test
    fun `given a paging query when function load of QueryablePagingSource is called then a LoadResult Page with data is returned`() =
        runTest {
            val items = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            val next: String? = null

            val pagingQuery: CursorPagingQuery<Int> = {
                Result.success(
                    CursorPage(
                        items = items,
                        next = next
                    )
                )
            }

            val cursorPagingSource = CursorPagingSource(pagingQuery = pagingQuery)
            val loadResult = cursorPagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = true
                )
            )

            assertTrue(
                "loadResult isn't a LoadResult.Page",
                loadResult is PagingSource.LoadResult.Page,
            )

            val page = loadResult as PagingSource.LoadResult.Page
            assertEquals(
                "Items aren't equal",
                items,
                page.data,
            )

            assertEquals(
                "Next token aren't equal",
                next,
                page.nextKey
            )
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `given a paging query that raises an exception when function load of QueryablePagingSource is called then a LoadResult Error is returned`() =
        runTest {
            val pagingQuery: CursorPagingQuery<Int> = {
                Result.failure(RuntimeException())
            }

            val cursorPagingSource = CursorPagingSource(pagingQuery = pagingQuery)
            val loadResult = cursorPagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )

            assertTrue(
                "loadResult isn't a LoadResult.Error",
                loadResult is PagingSource.LoadResult.Error,
            )
        }
}