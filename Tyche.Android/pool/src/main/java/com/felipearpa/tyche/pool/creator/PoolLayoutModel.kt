package com.felipearpa.tyche.pool.creator

import kotlinx.datetime.LocalDateTime

data class PoolLayoutModel(
    val id: String,
    val name: String,
    val startDateTime: LocalDateTime,
)
