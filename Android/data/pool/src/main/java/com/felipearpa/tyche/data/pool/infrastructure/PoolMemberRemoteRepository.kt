package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.pool.domain.PoolMember
import com.felipearpa.tyche.data.pool.domain.PoolMemberDataSource
import com.felipearpa.tyche.data.pool.domain.PoolMemberRepository
import com.felipearpa.tyche.data.pool.domain.toPoolMember

internal class PoolMemberRemoteRepository(
    private val poolMemberDataSource: PoolMemberDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler,
) : PoolMemberRepository {
    override suspend fun getPoolMembers(
        poolId: String,
        next: String?,
    ): Result<CursorPage<PoolMember>> {
        return networkExceptionHandler.handle {
            poolMemberDataSource.getPoolMembers(poolId = poolId, next = next)
                .map { poolMemberResponse -> poolMemberResponse.toPoolMember() }
        }
    }

    override suspend fun removeGambler(poolId: String, gamblerId: String): Result<Unit> {
        return networkExceptionHandler.handle {
            poolMemberDataSource.removeGambler(poolId = poolId, gamblerId = gamblerId)
        }
    }
}
