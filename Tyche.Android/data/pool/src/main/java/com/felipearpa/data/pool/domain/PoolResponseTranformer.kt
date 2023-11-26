package com.felipearpa.data.pool.domain

internal fun PoolResponse.toPool() =
    Pool(
        poolId = this.poolId,
        poolName = this.poolName
    )