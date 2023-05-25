package com.felipearpa.pool.domain

fun PoolResponse.toDomain() =
    Pool(
        poolId = this.poolId,
        poolName = this.poolName
    )