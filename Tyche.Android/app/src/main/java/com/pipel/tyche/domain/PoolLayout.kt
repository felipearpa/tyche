package com.pipel.tyche.domain

import com.pipel.core.type.DateTime
import com.pipel.core.type.NonEmptyString
import com.pipel.core.type.Uuid

data class PoolLayout(
    val poolLayoutId: Uuid,
    val name: NonEmptyString,
    val openingStartDateTime: DateTime,
    val openingEndDateTime: DateTime
)