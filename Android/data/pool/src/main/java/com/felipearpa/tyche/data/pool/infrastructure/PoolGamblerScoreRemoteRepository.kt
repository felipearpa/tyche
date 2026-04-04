package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScore
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreDataSource
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreRepository
import com.felipearpa.tyche.data.pool.domain.toPoolGamblerScore

internal class PoolGamblerScoreRemoteRepository(
    private val poolGamblerScoreDataSource: PoolGamblerScoreDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler,
) :
    PoolGamblerScoreRepository {

    override suspend fun getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerScore>> {
        return networkExceptionHandler.handle {
            poolGamblerScoreDataSource.getPoolGamblerScoresByGambler(
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
            poolGamblerScoreDataSource.getPoolGamblerScoresByPool(
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
            poolGamblerScoreDataSource.getPoolGamblerScore(
                poolId = poolId,
                gamblerId = gamblerId,
            ).toPoolGamblerScore()
        }
    }
}
