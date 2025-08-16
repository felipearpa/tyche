package com.felipearpa.tyche.data.bet.application

import com.felipearpa.tyche.data.bet.domain.Bet
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository
import javax.inject.Inject

class BetUseCase @Inject constructor(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(bet: Bet) =
        poolGamblerBetRepository.bet(bet = bet)
}
