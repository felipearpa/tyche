package com.felipearpa.pool.infrastructure

import com.felipearpa.core.network.NetworkExceptionHandler
import com.felipearpa.core.network.recoverNetworkException
import com.felipearpa.core.paging.CursorPage
import com.felipearpa.pool.domain.PoolGamblerScore
import com.felipearpa.pool.domain.PoolGamblerScoreRemoteDataSource
import com.felipearpa.pool.domain.PoolGamblerScoreRepository
import com.felipearpa.pool.domain.toDomain
import javax.inject.Inject

class PoolGamblerScoreRemoteRepository @Inject constructor(
    private val poolGamblerScoreRemoteDataSource: PoolGamblerScoreRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) :
    PoolGamblerScoreRepository {

    override suspend fun getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String?,
        searchText: String?
    ): Result<CursorPage<PoolGamblerScore>> {
        return networkExceptionHandler.handle {
            poolGamblerScoreRemoteDataSource.getPoolGamblerScoresByGambler(
                gamblerId = gamblerId,
                next = next,
                searchText = searchText
            ).map { gamblerPoolResponse -> gamblerPoolResponse.toDomain() }
        }.recoverNetworkException { exception ->
            return Result.failure(exception)
        }
    }

    override suspend fun getPoolGamblerScoresByPool(
        poolId: String,
        next: String?,
        searchText: String?
    ): Result<CursorPage<PoolGamblerScore>> {
        return networkExceptionHandler.handle {
            poolGamblerScoreRemoteDataSource.getPoolGamblerScoreByPool(
                poolId = poolId,
                next = next,
                searchText = searchText
            ).map { gamblerPoolResponse -> gamblerPoolResponse.toDomain() }
        }.recoverNetworkException { exception ->
            return Result.failure(exception)
        }
    }
}