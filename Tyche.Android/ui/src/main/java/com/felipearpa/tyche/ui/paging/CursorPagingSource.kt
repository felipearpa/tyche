package com.felipearpa.tyche.ui.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState

typealias CursorPagingQuery<T> = suspend (String?) -> Result<com.felipearpa.tyche.core.paging.CursorPage<T>>

open class CursorPagingSource<T : Any>(private val pagingQuery: CursorPagingQuery<T>) :
    PagingSource<String, T>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, T> {
        return pagingQuery(params.key).fold(
            onSuccess = { page ->
                LoadResult.Page(
                    data = page.items,
                    prevKey = null,
                    nextKey = page.next
                )
            },
            onFailure = { exception ->
                LoadResult.Error(exception)
            }
        )
    }

    override fun getRefreshKey(state: PagingState<String, T>): String? = null
}