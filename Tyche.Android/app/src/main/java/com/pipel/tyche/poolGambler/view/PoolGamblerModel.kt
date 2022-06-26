package com.pipel.tyche.poolGambler.view

import com.pipel.tyche.view.Progress

data class PoolGamblerModel(
    val poolId: String,
    val gamblerId: String,
    val gamblerEmail: String,
    val score: Int?,
    val progress: Progress
)