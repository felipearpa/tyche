package com.felipearpa.tyche.data.bet.application

import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.bet.domain.KickoffGroup
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository

/**
 * One page of a gambler's History: whole kickoff groups of computed matches, newest kickoff first.
 * A `null` cursor requests the first page. A rejected continuation fails with
 * `KickoffGroupFeedException.CursorRejected`.
 */
class GetFinishedPoolGamblerBets(private val poolGamblerBetRepository: PoolGamblerBetRepository) {
    suspend fun execute(
        poolId: String,
        gamblerId: String,
        next: String? = null,
    ): Result<CursorPage<KickoffGroup>> =
        poolGamblerBetRepository.getFinishedPoolGamblerBets(
            poolId = poolId,
            gamblerId = gamblerId,
            next = next,
        )
}
