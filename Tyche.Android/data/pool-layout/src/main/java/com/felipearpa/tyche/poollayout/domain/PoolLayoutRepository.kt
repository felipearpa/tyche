package com.felipearpa.tyche.poollayout.domain

import com.felipearpa.tyche.core.paging.CursorPage

internal interface PoolLayoutRepository {
    suspend fun getOpenPoolLayouts(): Result<CursorPage<PoolLayout>>
}