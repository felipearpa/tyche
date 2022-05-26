package com.pipel.tyche.pool.domain

import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.PositiveInt
import com.pipel.core.type.Ulid

data class Pool(
    val poolId: Ulid,
    val poolLayoutId: Ulid,
    val poolName: NonEmptyString,
    val currentPosition: PositiveInt?,
    val beforePosition: PositiveInt?
)