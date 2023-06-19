package com.felipearpa.tyche.pool.domain

data class Pool(
    val poolId: String,
    val poolName: String
)

fun PoolResponse.toPool() =
    Pool(
        poolId = this.poolId,
        poolName = this.poolName
    )