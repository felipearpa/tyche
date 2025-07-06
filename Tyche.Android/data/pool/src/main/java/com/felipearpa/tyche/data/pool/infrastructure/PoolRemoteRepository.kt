package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.tyche.data.pool.domain.CreatePoolInput
import com.felipearpa.tyche.data.pool.domain.Pool
import com.felipearpa.tyche.data.pool.domain.PoolRemoteDataSource
import com.felipearpa.tyche.data.pool.domain.PoolRepository
import com.felipearpa.tyche.data.pool.domain.toCreatePoolRequest
import com.felipearpa.tyche.data.pool.domain.toPool
import javax.inject.Inject

internal class PoolRemoteRepository @Inject constructor(
    private val poolRemoteDataSource: PoolRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler,
) : PoolRepository {
    override suspend fun createPool(createPoolInput: CreatePoolInput): Result<Pool> {
        return networkExceptionHandler.handle {
            poolRemoteDataSource.createPool(createPoolRequest = createPoolInput.toCreatePoolRequest())
                .toPool()
        }
    }
}
