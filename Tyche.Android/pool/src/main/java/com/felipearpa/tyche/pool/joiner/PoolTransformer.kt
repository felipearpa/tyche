package com.felipearpa.tyche.pool.joiner

import com.felipearpa.tyche.data.pool.domain.Pool

internal fun Pool.toPoolModel() =
    PoolModel(
        id = id,
        name = name,
    )
