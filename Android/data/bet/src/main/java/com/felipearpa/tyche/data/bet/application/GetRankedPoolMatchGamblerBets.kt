package com.felipearpa.tyche.data.bet.application

import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository

/**
 * One page of a match's participant bets, ordered by each participant's total points in the
 * pool, greatest first, with gambler ID breaking ties. The server's order is final: callers do
 * not sort pages. A `null` cursor requests the first page.
 *
 * Failures specific to this mode arrive as `RankedMatchBetsException`; other failures keep the
 * repository's usual exceptions.
 */
class GetRankedPoolMatchGamblerBets(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(
        poolId: String,
        matchId: String,
        next: String? = null,
    ): Result<CursorPage<PoolGamblerBet>> =
        poolGamblerBetRepository.getRankedPoolMatchGamblerBets(
            poolId = poolId,
            matchId = matchId,
            next = next,
        )
}
