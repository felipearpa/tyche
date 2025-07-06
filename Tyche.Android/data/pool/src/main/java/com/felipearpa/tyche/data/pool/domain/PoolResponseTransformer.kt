package com.felipearpa.tyche.data.pool.domain

internal fun CreatePoolResponse.toCreatePoolOutput() =
    CreatePoolOutput(
        poolId = poolId,
        poolName = poolName,
    )
