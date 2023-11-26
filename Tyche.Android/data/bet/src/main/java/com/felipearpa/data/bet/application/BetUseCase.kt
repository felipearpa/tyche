package com.felipearpa.data.bet.application

import com.felipearpa.data.bet.domain.Bet
import com.felipearpa.data.bet.domain.PoolGamblerBetRepository
import javax.inject.Inject

class BetUseCase @Inject constructor(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(bet: Bet) =
        poolGamblerBetRepository.bet(bet = bet)
}