package com.pipel.tyche.pool.data

data class PoolResponse(
    val poolId: String,
    val poolLayoutId: String,
    val poolName: String,
    val currentPosition: Int?,
    val beforePosition: Int?
)