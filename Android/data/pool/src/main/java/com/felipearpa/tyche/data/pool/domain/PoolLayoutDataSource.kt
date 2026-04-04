package com.felipearpa.tyche.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage

internal interface PoolLayoutDataSource {
    suspend fun getOpenPoolLayouts(
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolLayoutResponse>
}
