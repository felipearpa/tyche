package com.pipel.ui

import androidx.paging.PagingSource
import com.pipel.core.Page
import com.pipel.core.PagingQuery
import com.pipel.ui.paging.QueryablePagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class QueryablePagingSourceTest {

    @InjectMocks
    private lateinit var queryablePagingSource: QueryablePagingSource<Int>

    @Mock
    private lateinit var pagingQuery: PagingQuery<Int>

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `given a paging query when function load of QueryablePagingSource is called then a LoadResult Page is returned`() =
        runBlockingTest {
            `when`(pagingQuery.execute(anyInt(), anyInt())).thenReturn(Page.empty())
            val loadResult = queryablePagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )
            assertTrue(
                loadResult is PagingSource.LoadResult.Page,
                "loadResult is not a LoadResult.Page"
            )
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `given a paging query that raises an exception when function load of QueryablePagingSource is called then a LoadResult Error is returned`() =
        runBlockingTest {
            `when`(pagingQuery.execute(anyInt(), anyInt())).thenThrow()
            val loadResult = queryablePagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )
            assertTrue(
                loadResult is PagingSource.LoadResult.Error,
                "loadResult is not a LoadResult.Error"
            )
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `given a paging query where initialKey is between loadSize and allItemsCount when function load of QueryablePagingSource is called then a LoadResult's prevKey is initialKey minus loadSize`() =
        runBlockingTest {
            val initialKey = 15
            val loadSize = 10
            `when`(pagingQuery.execute(anyInt(), anyInt())).thenReturn(
                Page(
                    items = emptyList(),
                    itemsCount = 0,
                    skip = 0,
                    take = 0,
                    hasNext = false
                )
            )
            val loadResult = queryablePagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = initialKey,
                    loadSize = loadSize,
                    placeholdersEnabled = false
                )
            )
            assertTrue(
                loadResult is PagingSource.LoadResult.Page,
                "loadResult is not a LoadResult.Page"
            )
            assertEquals(
                initialKey - loadSize,
                (loadResult as PagingSource.LoadResult.Page).prevKey
            )
        }

    @ExperimentalCoroutinesApi
    @ParameterizedTest
    @MethodSource("valuesForPrevKeyNull")
    fun `given a PagingQuery where initialKey is null or zero when function load of QueryablePagingSource is called then LoadResult's prevKey is null`(
        initialKey: Int?
    ) =
        runBlockingTest {
            `when`(pagingQuery.execute(anyInt(), anyInt())).thenReturn(Page.empty())
            val loadResult = queryablePagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = initialKey,
                    loadSize = 0,
                    placeholdersEnabled = false
                )
            )
            assertTrue(
                loadResult is PagingSource.LoadResult.Page,
                "loadResult is not a LoadResult.Page"
            )
            assertNull((loadResult as PagingSource.LoadResult.Page).prevKey)
        }

    @ExperimentalCoroutinesApi
    @ParameterizedTest
    @MethodSource("valuesForPrevKeyNotNull")
    fun `given a PagingQuery where initialKey minus loadSize is less or equal than 0 when function load of QueryablePagingSource is called then LoadResult's prevKey is 0`(
        initialKey: Int,
        loadSize: Int
    ) =
        runBlockingTest {
            `when`(pagingQuery.execute(anyInt(), anyInt())).thenReturn(Page.empty())
            val loadResult = queryablePagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = initialKey,
                    loadSize = loadSize,
                    placeholdersEnabled = false
                )
            )
            assertTrue(
                loadResult is PagingSource.LoadResult.Page,
                "loadResult is not a LoadResult.Page"
            )
            val page = loadResult as PagingSource.LoadResult.Page
            assertEquals(0, page.prevKey)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `given a PagingQuery where hasNext is false when function load of QueryablePagingSource is called then LoadResult's nextKey is null`() =
        runBlockingTest {
            `when`(pagingQuery.execute(anyInt(), anyInt())).thenReturn(
                Page(
                    items = emptyList(),
                    itemsCount = 0,
                    skip = 0,
                    take = 0,
                    hasNext = false
                )
            )
            val loadResult = queryablePagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = 0,
                    loadSize = 10,
                    placeholdersEnabled = false
                )
            )
            assertTrue(
                loadResult is PagingSource.LoadResult.Page,
                "loadResult is not a LoadResult.Page"
            )
            val page = loadResult as PagingSource.LoadResult.Page
            assertNull(page.nextKey)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `given a PagingQuery where hasNext is true when function load of QueryablePagingSource is called then LoadResult's nextKey is loadSize plus initialKey`() =
        runBlockingTest {
            val initialKey = 5
            val loadSize = 10
            `when`(pagingQuery.execute(anyInt(), anyInt())).thenReturn(
                Page(
                    items = emptyList(),
                    itemsCount = 0,
                    skip = 0,
                    take = 0,
                    hasNext = true
                )
            )
            val loadResult = queryablePagingSource.load(
                PagingSource.LoadParams.Refresh(
                    key = initialKey,
                    loadSize = loadSize,
                    placeholdersEnabled = false
                )
            )
            assertTrue(
                loadResult is PagingSource.LoadResult.Page,
                "loadResult is not a LoadResult.Page"
            )
            val page = loadResult as PagingSource.LoadResult.Page
            assertEquals(initialKey + loadSize, page.nextKey)
        }

    companion object {
        @JvmStatic
        fun valuesForPrevKeyNull(): Array<Array<Int?>> {
            return arrayOf(arrayOf(0), arrayOf(null))
        }

        @JvmStatic
        fun valuesForPrevKeyNotNull(): Array<Array<Int>> {
            return arrayOf(arrayOf(1, 10), arrayOf(5, 10), arrayOf(9, 10), arrayOf(10, 10))
        }
    }

}