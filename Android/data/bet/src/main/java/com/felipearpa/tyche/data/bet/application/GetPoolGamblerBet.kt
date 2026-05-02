package com.felipearpa.tyche.data.bet.application

import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository

class GetPoolGamblerBet(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(
        poolId: String,
        gamblerId: String,
        matchId: String,
    ) =
        poolGamblerBetRepository.getPoolGamblerBet(
            poolId = poolId,
            gamblerId = gamblerId,
            matchId = matchId,
        )
}
