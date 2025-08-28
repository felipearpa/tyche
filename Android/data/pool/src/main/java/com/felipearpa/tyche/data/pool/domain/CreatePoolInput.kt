package com.felipearpa.tyche.data.pool.domain

import com.felipearpa.tyche.data.pool.type.PoolName

data class CreatePoolInput(
    val poolLayoutId: String,
    val poolName: PoolName,
    val ownerGamblerId: String,
)
