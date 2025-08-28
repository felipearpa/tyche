package com.felipearpa.tyche.pool.creator

import com.felipearpa.tyche.data.pool.domain.CreatePoolInput
import com.felipearpa.tyche.data.pool.type.PoolName

internal fun CreatePoolModel.toCreatePoolInput(ownerGamblerId: String) =
    CreatePoolInput(
        poolLayoutId = poolLayoutId,
        poolName = PoolName(poolName),
        ownerGamblerId = ownerGamblerId,
    )
