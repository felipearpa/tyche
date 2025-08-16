package com.felipearpa.tyche.pool.creator

data class CreatePoolModel(
    val poolLayoutId: String,
    val poolName: String,
    val poolId: String?,
)

private val emptyCreatePoolModel = CreatePoolModel(
    poolLayoutId = "",
    poolName = "",
    poolId = null,
)

fun emptyCreatePoolModel() = emptyCreatePoolModel
