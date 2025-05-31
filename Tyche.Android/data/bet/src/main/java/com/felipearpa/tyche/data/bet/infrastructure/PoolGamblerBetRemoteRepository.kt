package com.felipearpa.tyche.data.bet.infrastructure

import com.felipearpa.tyche.data.bet.domain.Bet
import com.felipearpa.tyche.data.bet.domain.BetException
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRemoteDataSource
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository
import com.felipearpa.tyche.data.bet.domain.toBetRequest
import com.felipearpa.tyche.data.bet.domain.toPoolGamblerBet
import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverHttpException
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.paging.map
import javax.inject.Inject

internal class PoolGamblerBetRemoteRepository @Inject constructor(
    private val poolGamblerBetRemoteDataSource: PoolGamblerBetRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler,
) : PoolGamblerBetRepository {
    override suspend fun getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerBet>> {
        return networkExceptionHandler.handle {
            poolGamblerBetRemoteDataSource.getPendingPoolGamblerBets(
                poolId = poolId,
                gamblerId = gamblerId,
                next = next,
                searchText = searchText,
            ).map { poolGamblerBetResponse -> poolGamblerBetResponse.toPoolGamblerBet() }
        }
    }

    override suspend fun getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerBet>> {
        return networkExceptionHandler.handle {
            poolGamblerBetRemoteDataSource.getFinishedPoolGamblerBets(
                poolId = poolId,
                gamblerId = gamblerId,
                next = next,
                searchText = searchText,
            ).map { poolGamblerBetResponse -> poolGamblerBetResponse.toPoolGamblerBet() }
        }
    }

    override suspend fun bet(bet: Bet): Result<PoolGamblerBet> {
        return networkExceptionHandler.handle {
            poolGamblerBetRemoteDataSource.bet(betRequest = bet.toBetRequest()).toPoolGamblerBet()
        }.recoverHttpException { exception ->
            when (exception.httpStatusCode) {
                HttpStatusCode.FORBIDDEN -> BetException.Forbidden
                else -> exception
            }
        }
    }
}
