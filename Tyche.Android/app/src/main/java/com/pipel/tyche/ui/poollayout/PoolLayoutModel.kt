package com.pipel.tyche.ui.poollayout

import java.util.*

data class PoolLayoutModel(
    val poolLayoutId: UUID,
    val name: String,
    val openingStartDateTime: Date,
    val openingEndDateTime: Date
)