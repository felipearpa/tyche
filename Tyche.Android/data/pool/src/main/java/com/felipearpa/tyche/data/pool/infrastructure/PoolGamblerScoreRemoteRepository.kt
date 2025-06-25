package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScore
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreRemoteDataSource
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreRepository
import com.felipearpa.tyche.data.pool.domain.toPoolGamblerScore
import javax.inject.Inject

internal class PoolGamblerScoreRemoteRepository @Inject constructor(
    private val poolGamblerScoreRemoteDataSource: PoolGamblerScoreRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler,
) :
    PoolGamblerScoreRepository {

    override suspend fun getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerScore>> {
        return networkExceptionHandler.handle {
            poolGamblerScoreRemoteDataSource.getPoolGamblerScoresByGambler(
                gamblerId = gamblerId,
                next = next,
                searchText = searchText,
            ).map { gamblerPoolResponse -> gamblerPoolResponse.toPoolGamblerScore() }
        }
    }

    override suspend fun getPoolGamblerScoresByPool(
        poolId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerScore>> {
        return networkExceptionHandler.handle {
            poolGamblerScoreRemoteDataSource.getPoolGamblerScoresByPool(
                poolId = poolId,
                next = next,
                searchText = searchText,
            ).map { gamblerPoolResponse -> gamblerPoolResponse.toPoolGamblerScore() }
        }
    }

    override suspend fun getPoolGamblerScore(
        poolId: String,
        gamblerId: String,
    ): Result<PoolGamblerScore> {
        return networkExceptionHandler.handle {
            poolGamblerScoreRemoteDataSource.getPoolGamblerScore(
                poolId = poolId,
                gamblerId = gamblerId,
            ).toPoolGamblerScore()
        }
    }
}
