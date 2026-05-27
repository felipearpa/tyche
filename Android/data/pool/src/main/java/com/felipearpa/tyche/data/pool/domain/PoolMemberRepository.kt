package com.felipearpa.tyche.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage

internal interface PoolMemberRepository {
    suspend fun getPoolMembers(
        poolId: String,
        next: String? = null,
    ): Result<CursorPage<PoolMember>>

    suspend fun removeGambler(poolId: String, gamblerId: String): Result<Unit>
}
