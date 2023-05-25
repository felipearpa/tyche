package com.felipearpa.pool.ui

import com.felipearpa.pool.domain.Pool

fun Pool.toModel() =
    PoolModel(
        poolId = this.poolId,
        poolName = this.poolName
    )