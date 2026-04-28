package com.felipearpa.tyche.matchbets

import kotlinx.serialization.Serializable

@Serializable
data class MatchBetsViewRoute(
    val poolId: String,
    val matchId: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val matchDateTimeIso: String,
    val homeTeamScore: Int? = null,
    val awayTeamScore: Int? = null,
)
