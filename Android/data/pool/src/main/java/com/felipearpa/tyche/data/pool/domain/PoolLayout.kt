package com.felipearpa.tyche.data.pool.domain

import kotlinx.datetime.LocalDateTime

data class PoolLayout(
    val id: String,
    val name: String,
    val startDateTime: LocalDateTime,
)
