package com.felipearpa.tyche.data.pool.domain

data class CreatePoolInput(
    val poolLayoutId: String,
    val poolName: String,
    val ownerGamblerId: String,
)
