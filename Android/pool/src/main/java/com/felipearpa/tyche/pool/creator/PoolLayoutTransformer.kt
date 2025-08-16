package com.felipearpa.tyche.pool.creator

import com.felipearpa.tyche.data.pool.domain.PoolLayout

fun PoolLayout.toPoolLayoutModel() = PoolLayoutModel(
    id = id,
    name = name,
    startDateTime = startDateTime,
)
