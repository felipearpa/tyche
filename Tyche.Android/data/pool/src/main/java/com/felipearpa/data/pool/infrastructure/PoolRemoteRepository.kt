package com.felipearpa.data.pool.infrastructure

import com.felipearpa.data.pool.domain.Pool
import com.felipearpa.data.pool.domain.PoolRemoteDataSource
import com.felipearpa.data.pool.domain.PoolRepository
import com.felipearpa.data.pool.domain.toPool
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import javax.inject.Inject

internal class PoolRemoteRepository @Inject constructor(
    private val poolRemoteDataSource: PoolRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    PoolRepository {

    override suspend fun getPool(poolId: String): Result<Pool> {
        return networkExceptionHandler.handle {
            poolRemoteDataSource.getPool(poolId = poolId).toPool()
        }
    }
}