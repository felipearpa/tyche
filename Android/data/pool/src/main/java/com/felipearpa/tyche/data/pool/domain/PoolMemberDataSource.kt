package com.felipearpa.tyche.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage

internal interface PoolMemberDataSource {
    suspend fun getPoolMembers(
        poolId: String,
        next: String? = null,
    ): CursorPage<PoolMemberResponse>

    suspend fun removeGambler(poolId: String, gamblerId: String)
}
