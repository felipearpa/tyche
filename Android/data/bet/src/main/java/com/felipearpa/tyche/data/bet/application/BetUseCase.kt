package com.felipearpa.tyche.data.bet.application

import com.felipearpa.tyche.data.bet.domain.Bet
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository

class BetUseCase(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(bet: Bet) =
        poolGamblerBetRepository.bet(bet = bet)
}
