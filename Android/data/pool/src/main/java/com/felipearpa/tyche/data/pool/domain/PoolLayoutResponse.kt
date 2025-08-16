package com.felipearpa.tyche.data.pool.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
internal data class PoolLayoutResponse(
    val id: String,
    val name: String,
    @Contextual val startDateTime: LocalDateTime,
)
