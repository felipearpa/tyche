package com.felipearpa.tyche.pool.infrastructure

import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverNetworkException
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.pool.domain.PoolGamblerScore
import com.felipearpa.tyche.pool.domain.PoolGamblerScoreRemoteDataSource
import com.felipearpa.tyche.pool.domain.PoolGamblerScoreRepository
import com.felipearpa.tyche.pool.domain.toPoolGamblerScore
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
            ).map { gamblerPoolResponse -> gamblerPoolResponse.toPoolGamblerScore() }
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
            ).map { gamblerPoolResponse -> gamblerPoolResponse.toPoolGamblerScore() }
        }.recoverNetworkException { exception ->
            return Result.failure(exception)
        }
    }
}