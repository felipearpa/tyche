package com.felipearpa.tyche.pool

import com.felipearpa.data.pool.domain.Pool

fun Pool.toPoolModel() =
    PoolModel(
        poolId = poolId,
        poolName = poolName
    )