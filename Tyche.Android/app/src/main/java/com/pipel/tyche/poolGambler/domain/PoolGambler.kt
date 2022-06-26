package com.pipel.tyche.poolGambler.domain

import com.pipel.core.type.Email
import com.pipel.core.type.PositiveInt
import com.pipel.core.type.Ulid

data class PoolGambler(
    val poolId: Ulid,
    val gamblerId: Ulid,
    val gamblerEmail: Email,
    val score: PositiveInt?,
    val currentPosition: PositiveInt?,
    val beforePosition: PositiveInt?
)