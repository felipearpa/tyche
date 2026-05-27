package com.felipearpa.tyche.pool.managegamblers

import kotlinx.serialization.Serializable

@Serializable
data class ManageGamblersRoute(
    val poolId: String,
    val gamblerId: String,
)
