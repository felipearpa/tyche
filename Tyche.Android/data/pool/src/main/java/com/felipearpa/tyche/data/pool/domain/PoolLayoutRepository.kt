package com.felipearpa.tyche.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage

internal interface PoolLayoutRepository {
    suspend fun getOpenPoolLayouts(): Result<CursorPage<PoolLayout>>
}
