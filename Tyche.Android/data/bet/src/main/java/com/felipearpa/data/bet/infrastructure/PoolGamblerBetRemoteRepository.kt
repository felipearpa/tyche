package com.felipearpa.data.bet.infrastructure

import com.felipearpa.data.bet.domain.Bet
import com.felipearpa.data.bet.domain.BetException
import com.felipearpa.data.bet.domain.PoolGamblerBet
import com.felipearpa.data.bet.domain.PoolGamblerBetRemoteDataSource
import com.felipearpa.data.bet.domain.PoolGamblerBetRepository
import com.felipearpa.data.bet.domain.toBetRequest
import com.felipearpa.data.bet.domain.toPoolGamblerBet
import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverHttpException
import com.felipearpa.tyche.core.network.recoverNetworkException
import com.felipearpa.tyche.core.paging.CursorPage
import javax.inject.Inject

internal class PoolGamblerBetRemoteRepository @Inject constructor(
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
            ).map { poolGamblerBetResponse -> poolGamblerBetResponse.toPoolGamblerBet() }
        }.recoverNetworkException { exception ->
            return Result.failure(exception)
        }
    }

    override suspend fun bet(bet: Bet): Result<PoolGamblerBet> {
        return networkExceptionHandler.handle {
            poolGamblerBetRemoteDataSource.bet(betRequest = bet.toBetRequest()).toPoolGamblerBet()
        }.recoverHttpException { exception ->
            return when (exception.httpStatusCode) {
                HttpStatusCode.FORBIDDEN -> Result.failure(BetException.Forbidden)
                else -> Result.failure(exception)
            }
        }
    }
}