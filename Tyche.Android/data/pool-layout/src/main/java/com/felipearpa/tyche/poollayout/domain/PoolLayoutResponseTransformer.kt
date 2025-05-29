package com.felipearpa.tyche.poollayout.domain

internal fun PoolLayoutResponse.toPoolLayout() =
    PoolLayout(
        id = id,
        name = name,
        startDateTime = startDateTime,
    )