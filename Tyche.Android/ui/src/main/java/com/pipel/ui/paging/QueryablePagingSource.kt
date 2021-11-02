package com.pipel.ui.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pipel.core.PagingQuery
import kotlin.math.max

open class QueryablePagingSource<TModel : Any>(private val pagingQuery: PagingQuery<TModel>) :
    PagingSource<Int, TModel>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TModel> {
        val skip = params.key ?: 0
        val take = params.loadSize

        return try {
            val page = pagingQuery.execute(skip = skip, take = take)
            LoadResult.Page(
                data = page.items,
                prevKey = if (skip > 0) max(skip - take, 0) else null,
                nextKey = if (page.hasNext) skip + take else null
            )
        } catch (ex: Exception) {
            LoadResult.Error(ex)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}