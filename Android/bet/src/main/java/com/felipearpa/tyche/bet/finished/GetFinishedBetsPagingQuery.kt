package com.felipearpa.tyche.bet.finished

import com.felipearpa.tyche.bet.kickoffgroup.toKickoffGroupModel
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.bet.application.GetFinishedPoolGamblerBets

suspend fun getFinishedBetsPagingQuery(
    next: String?,
    poolId: String,
    gamblerId: String,
    getFinishedPoolGamblerBets: GetFinishedPoolGamblerBets,
) = getFinishedPoolGamblerBets.execute(
    poolId = poolId,
    gamblerId = gamblerId,
    next = next,
).map { page -> page.map { kickoffGroup -> kickoffGroup.toKickoffGroupModel() } }
