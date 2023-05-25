package com.felipearpa.pool.infrastructure

import com.felipearpa.core.network.NetworkExceptionHandler
import com.felipearpa.core.network.recoverNetworkException
import com.felipearpa.pool.domain.Pool
import com.felipearpa.pool.domain.PoolRemoteDataSource
import com.felipearpa.pool.domain.PoolRepository
import com.felipearpa.pool.domain.toDomain
import javax.inject.Inject

class PoolRemoteRepository @Inject constructor(
    private val poolRemoteDataSource: PoolRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    PoolRepository {

    override suspend fun getPool(poolId: String): Result<Pool> {
        return networkExceptionHandler.handle {
            poolRemoteDataSource.getPool(poolId = poolId).toDomain()
        }.recoverNetworkException { exception ->
            return Result.failure(exception)
        }
    }
}