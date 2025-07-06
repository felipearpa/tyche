package com.felipearpa.tyche.pool.creator

data class CreatePoolModel(
    val poolLayoutId: String,
    val poolName: String,
)

private val emptyCreatePoolModel = CreatePoolModel(
    poolLayoutId = "",
    poolName = "",
)

fun emptyCreatePoolModel() = emptyCreatePoolModel
