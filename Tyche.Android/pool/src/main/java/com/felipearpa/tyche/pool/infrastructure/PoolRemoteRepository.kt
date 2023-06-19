package com.felipearpa.tyche.pool.infrastructure

import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverNetworkException
import com.felipearpa.tyche.pool.domain.Pool
import com.felipearpa.tyche.pool.domain.PoolRemoteDataSource
import com.felipearpa.tyche.pool.domain.PoolRepository
import com.felipearpa.tyche.pool.domain.toPool
import javax.inject.Inject

class PoolRemoteRepository @Inject constructor(
    private val poolRemoteDataSource: PoolRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    PoolRepository {

    override suspend fun getPool(poolId: String): Result<Pool> {
        return networkExceptionHandler.handle {
            poolRemoteDataSource.getPool(poolId = poolId).toPool()
        }.recoverNetworkException { exception ->
            return Result.failure(exception)
        }
    }
}