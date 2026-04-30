package com.felipearpa.tyche.bet.match

import kotlinx.serialization.Serializable

@Serializable
data class MatchBetListViewRoute(
    val poolId: String,
    val matchId: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val matchDateTimeIso: String,
    val homeTeamScore: Int? = null,
    val awayTeamScore: Int? = null,
    val isLive: Boolean,
)
