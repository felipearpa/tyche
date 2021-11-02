package com.pipel.tyche.data

import java.util.*

data class PoolLayoutResponse(
    val poolLayoutId: UUID,
    val name: String,
    val openingStartDateTime: Date,
    val openingEndDateTime: Date
)