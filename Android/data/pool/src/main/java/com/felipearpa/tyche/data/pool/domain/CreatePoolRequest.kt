package com.felipearpa.tyche.data.pool.domain

import kotlinx.serialization.Serializable

@Serializable
data class CreatePoolRequest(
    val poolLayoutId: String,
    val poolName: String,
    val ownerGamblerId: String,
)
