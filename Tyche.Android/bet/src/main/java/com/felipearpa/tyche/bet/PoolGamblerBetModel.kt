package com.felipearpa.tyche.bet

import com.felipearpa.tyche.core.type.TeamScore
import java.time.LocalDateTime

data class PoolGamblerBetModel(
    val poolId: String,
    val gamblerId: String,
    val matchId: String,
    val homeTeamId: String,
    val homeTeamName: String,
    val awayTeamId: String,
    val awayTeamName: String,
    val matchScore: TeamScore<Int>?,
    var betScore: TeamScore<Int>?,
    val score: Int?,
    val matchDateTime: LocalDateTime
) {
    var isLockEnforced = false

    fun isLocked() = isLockEnforced || LocalDateTime.now() >= matchDateTime
}