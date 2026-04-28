package com.felipearpa.tyche.data.bet.infrastructure

import com.felipearpa.network.HttpStatus
import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.network.recoverHttpException
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.bet.domain.Bet
import com.felipearpa.tyche.data.bet.domain.BetException
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetDataSource
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository
import com.felipearpa.tyche.data.bet.domain.toBetRequest
import com.felipearpa.tyche.data.bet.domain.toPoolGamblerBet

internal class PoolGamblerBetRemoteRepository(
    private val poolGamblerBetDataSource: PoolGamblerBetDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler,
) : PoolGamblerBetRepository {

    override suspend fun getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerBet>> {
        return networkExceptionHandler.handle {
            poolGamblerBetDataSource.getPendingPoolGamblerBets(
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
            poolGamblerBetDataSource.getFinishedPoolGamblerBets(
                poolId = poolId,
                gamblerId = gamblerId,
                next = next,
                searchText = searchText,
            ).map { poolGamblerBetResponse -> poolGamblerBetResponse.toPoolGamblerBet() }
        }
    }

    override suspend fun getLivePoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerBet>> {
        return networkExceptionHandler.handle {
            poolGamblerBetDataSource.getLivePoolGamblerBets(
                poolId = poolId,
                gamblerId = gamblerId,
                next = next,
                searchText = searchText,
            ).map { poolGamblerBetResponse -> poolGamblerBetResponse.toPoolGamblerBet() }
        }
    }

    override suspend fun bet(bet: Bet): Result<PoolGamblerBet> {
        return networkExceptionHandler.handle {
            poolGamblerBetDataSource.bet(betRequest = bet.toBetRequest()).toPoolGamblerBet()
        }.recoverHttpException { exception ->
            when (exception.httpStatus) {
                HttpStatus.FORBIDDEN -> BetException.Forbidden
                else -> exception
            }
        }
    }
}
