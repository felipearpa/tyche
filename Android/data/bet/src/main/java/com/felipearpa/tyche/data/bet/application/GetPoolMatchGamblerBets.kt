package com.felipearpa.tyche.data.bet.application

import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository

class GetPoolMatchGamblerBets(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(
        poolId: String,
        matchId: String,
        next: String? = null,
    ) =
        poolGamblerBetRepository.getPoolMatchGamblerBets(
            poolId = poolId,
            matchId = matchId,
            next = next,
        )
}
