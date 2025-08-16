package com.felipearpa.tyche.data.pool.domain

internal fun PoolLayoutResponse.toPoolLayout() =
    PoolLayout(
        id = id,
        name = name,
        startDateTime = startDateTime,
    )
