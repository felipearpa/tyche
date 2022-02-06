package com.pipel.tyche.poolLayout.data

import java.util.*

data class PoolLayoutResponse(
    val poolLayoutId: String,
    val name: String,
    val startOpeningDateTime: Date,
    val endOpeningDateTime: Date
)