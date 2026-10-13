package com.felipearpa.tyche.data.bet.application

import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.bet.domain.KickoffGroup
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository

/**
 * One page of a gambler's Timeline: whole kickoff groups, unsettled groups first and then finished
 * ones, in the server's order. A `null` cursor requests the first page. A rejected continuation
 * fails with `KickoffGroupFeedException.CursorRejected`.
 */
class GetGamblerBetsTimeline(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(
        poolId: String,
        gamblerId: String,
        next: String? = null,
    ): Result<CursorPage<KickoffGroup>> =
        poolGamblerBetRepository.getGamblerBetsTimeline(
            poolId = poolId,
            gamblerId = gamblerId,
            next = next,
        )
}
