package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.tyche.data.pool.domain.CreatePoolInput
import com.felipearpa.tyche.data.pool.domain.CreatePoolOutput
import com.felipearpa.tyche.data.pool.domain.JoinPoolInput
import com.felipearpa.tyche.data.pool.domain.Pool
import com.felipearpa.tyche.data.pool.domain.PoolDataSource
import com.felipearpa.tyche.data.pool.domain.PoolRepository
import com.felipearpa.tyche.data.pool.domain.toCreatePoolOutput
import com.felipearpa.tyche.data.pool.domain.toCreatePoolRequest
import com.felipearpa.tyche.data.pool.domain.toJoinPoolRequest
import com.felipearpa.tyche.data.pool.domain.toPool

internal class PoolRemoteRepository(
    private val poolDataSource: PoolDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler,
) : PoolRepository {
    override suspend fun getPool(id: String): Result<Pool> {
        return networkExceptionHandler.handle {
            poolDataSource.getPool(id = id).toPool()
        }
    }

    override suspend fun createPool(createPoolInput: CreatePoolInput): Result<CreatePoolOutput> {
        return networkExceptionHandler.handle {
            poolDataSource.createPool(createPoolRequest = createPoolInput.toCreatePoolRequest())
                .toCreatePoolOutput()
        }
    }

    override suspend fun joinPool(joinPoolInput: JoinPoolInput): Result<Unit> {
        return networkExceptionHandler.handle {
            poolDataSource.joinPool(
                poolId = joinPoolInput.poolId,
                joinPoolRequest = joinPoolInput.toJoinPoolRequest(),
            )
        }
    }
}
