package com.felipearpa.tyche.ui.paging

import androidx.paging.PagingSource
import com.felipearpa.tyche.core.paging.CursorPage
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class QueryablePagingSourceTest {
    @Test
    fun `given a paging query when the data is loaded then a Page LoadResult with data is returned`() =
        runTest {
            val pagingQuery = `given a paging query`()
            val loadResult = `when the data is loaded`(pagingQuery = pagingQuery)
            `then a Page LoadResult with data is returned`(actualLoadResult = loadResult)
        }

    @Test
    fun `given a causing exception paging query when the data is loaded then an Error LoadResult is returned`() =
        runTest {
            val pagingQuery = `given a causing exception paging query`()
            val loadResult = `when the data is loaded`(pagingQuery = pagingQuery)
            `then an Error LoadResult is returned`(actualLoadResult = loadResult)
        }

    private fun `given a paging query`(): CursorPagingQuery<Int> {
        val pagingQuery: CursorPagingQuery<Int> = {
            Result.success(
                CursorPage(
                    items = items,
                    next = next
                )
            )
        }
        return pagingQuery
    }

    private suspend fun `when the data is loaded`(pagingQuery: CursorPagingQuery<Int>): PagingSource.LoadResult<String, Int> {
        val cursorPagingSource = CursorPagingSource(pagingQuery = pagingQuery)
        return cursorPagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = true
            )
        )
    }

    private fun `then a Page LoadResult with data is returned`(actualLoadResult: PagingSource.LoadResult<String, Int>) {
        actualLoadResult.shouldBeInstanceOf<PagingSource.LoadResult.Page<*, *>>()

        val page = actualLoadResult as PagingSource.LoadResult.Page
        page.data shouldContainExactly items
        page.nextKey shouldBe next
    }

    private fun `given a causing exception paging query`(): CursorPagingQuery<Int> {
        val pagingQuery: CursorPagingQuery<Int> = {
            Result.failure(RuntimeException())
        }
        return pagingQuery
    }

    private fun `then an Error LoadResult is returned`(actualLoadResult: PagingSource.LoadResult<String, Int>) {
        actualLoadResult.shouldBeInstanceOf<PagingSource.LoadResult.Error<*, *>>()
    }
}

private val items = (0..9).toList()
private val next: String? = null