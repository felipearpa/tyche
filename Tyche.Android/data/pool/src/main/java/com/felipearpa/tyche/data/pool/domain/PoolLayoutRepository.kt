package com.felipearpa.tyche.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage

internal interface PoolLayoutRepository {
    suspend fun getOpenPoolLayouts(
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolLayout>>
}
