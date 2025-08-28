package com.felipearpa.tyche.data.pool.domain

internal fun CreatePoolInput.toCreatePoolRequest() =
    CreatePoolRequest(
        poolLayoutId = poolLayoutId,
        poolName = poolName.value,
        ownerGamblerId = ownerGamblerId,
    )
