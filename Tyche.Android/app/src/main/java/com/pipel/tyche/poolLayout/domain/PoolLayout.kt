package com.pipel.tyche.poolLayout.domain

import com.pipel.core.type.DateTime
import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.Ulid

data class PoolLayout(
    val poolLayoutId: Ulid,
    val name: NonEmptyString,
    val openingStartDateTime: DateTime,
    val openingEndDateTime: DateTime
)