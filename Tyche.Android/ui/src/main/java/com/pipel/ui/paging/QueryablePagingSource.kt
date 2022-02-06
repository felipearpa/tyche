package com.pipel.ui.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pipel.core.PagingQuery

open class QueryablePagingSource<TModel : Any>(private val pagingQuery: PagingQuery<TModel>) :
    PagingSource<String, TModel>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, TModel> {
        return try {
            val page = pagingQuery.execute(params.key)
            LoadResult.Page(
                data = page.items,
                prevKey = null,
                nextKey = page.nextToken
            )
        } catch (ex: Exception) {
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<String, TModel>): String? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPageIndex = state.pages.indexOf(state.closestPageToPosition(anchorPosition))
            state.pages.getOrNull(anchorPageIndex + 1)?.prevKey ?: state.pages.getOrNull(
                anchorPageIndex - 1
            )?.nextKey
        }
    }
}