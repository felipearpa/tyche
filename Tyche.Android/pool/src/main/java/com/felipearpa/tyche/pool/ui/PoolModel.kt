package com.felipearpa.tyche.pool.ui

import com.felipearpa.tyche.pool.domain.Pool

data class PoolModel(
    val poolId: String,
    val poolName: String
)

fun Pool.toPoolModel() =
    PoolModel(
        poolId = this.poolId,
        poolName = this.poolName
    )