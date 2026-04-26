package com.felipearpa.tyche.pool.creator

import kotlinx.serialization.Serializable

@Serializable
data class PoolFromLayoutCreatorRoute(
    val gamblerId: String,
    val preselectedPoolLayoutId: String? = null,
    val preselectedPoolName: String? = null,
)
