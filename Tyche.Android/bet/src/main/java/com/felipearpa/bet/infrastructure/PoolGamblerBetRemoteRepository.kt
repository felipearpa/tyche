package com.felipearpa.bet.infrastructure

import com.felipearpa.bet.domain.Bet
import com.felipearpa.bet.domain.BetException
import com.felipearpa.bet.domain.PoolGamblerBet
import com.felipearpa.bet.domain.PoolGamblerBetRemoteDataSource
import com.felipearpa.bet.domain.PoolGamblerBetRepository
import com.felipearpa.bet.domain.toDomain
import com.felipearpa.bet.domain.toRequest
import com.felipearpa.core.network.HttpStatusCode
import com.felipearpa.core.network.NetworkExceptionHandler
import com.felipearpa.core.network.recoverHttpException
import com.felipearpa.core.network.recoverNetworkException
import com.felipearpa.core.paging.CursorPage
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