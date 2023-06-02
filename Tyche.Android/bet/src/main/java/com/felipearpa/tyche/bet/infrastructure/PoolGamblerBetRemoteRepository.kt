package com.felipearpa.tyche.bet.infrastructure

import com.felipearpa.tyche.bet.domain.Bet
import com.felipearpa.tyche.bet.domain.BetException
import com.felipearpa.tyche.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.bet.domain.PoolGamblerBetRemoteDataSource
import com.felipearpa.tyche.bet.domain.PoolGamblerBetRepository
import com.felipearpa.tyche.bet.domain.toDomain
import com.felipearpa.tyche.bet.domain.toRequest
import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverHttpException
import com.felipearpa.tyche.core.network.recoverNetworkException
import com.felipearpa.tyche.core.paging.CursorPage
import javax.inject.Inject

class PoolGamblerBetRemoteRepository @Inject constructor(
    private val poolGamblerBetRemoteDataSource: PoolGamblerBetRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) : PoolGamblerBetRepository {

    override suspend fun getPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?
    ): Result<CursorPage<PoolGamblerBet>> {
        return networkExceptionHandler.handle {
            poolGamblerBetRemoteDataSource.getPoolGamblerBets(
                poolId = poolId,
                gamblerId = gamblerId,
                next = next,
                searchText = searchText
            ).map { poolGamblerBetResponse -> poolGamblerBetResponse.toDomain() }
        }.recoverNetworkException { exception ->
            return Result.failure(exception)
        }
    }

    override suspend fun bet(bet: Bet): Result<PoolGamblerBet> {
        return networkExceptionHandler.handle {
            poolGamblerBetRemoteDataSource.bet(betRequest = bet.toRequest()).toDomain()
        }.recoverHttpException { exception ->
            return when (exception.httpStatusCode) {
                HttpStatusCode.FORBIDDEN -> Result.failure(BetException.Forbidden)
                else -> Result.failure(exception)
            }
        }
    }
}