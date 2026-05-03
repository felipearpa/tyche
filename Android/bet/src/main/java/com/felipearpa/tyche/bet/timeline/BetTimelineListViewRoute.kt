package com.felipearpa.tyche.bet.timeline

import kotlinx.serialization.Serializable

@Serializable
data class BetTimelineListViewRoute(
    val poolId: String,
    val gamblerId: String,
    val gamblerUsername: String,
)
