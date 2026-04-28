package com.felipearpa.tyche.data.bet.application

import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository

class GetGamblerBetsTimeline(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(
        poolId: String,
        gamblerId: String,
        next: String? = null,
    ) =
        poolGamblerBetRepository.getGamblerBetsTimeline(
            poolId = poolId,
            gamblerId = gamblerId,
            next = next,
        )
}
