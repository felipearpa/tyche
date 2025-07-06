package com.felipearpa.tyche.data.pool.domain

internal fun PoolResponse.toPool() =
    Pool(
        id = poolId,
        name = poolName,
    )
