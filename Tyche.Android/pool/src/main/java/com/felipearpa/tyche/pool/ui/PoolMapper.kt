package com.felipearpa.tyche.pool.ui

import com.felipearpa.tyche.pool.domain.Pool

fun Pool.toModel() =
    PoolModel(
        poolId = this.poolId,
        poolName = this.poolName
    )