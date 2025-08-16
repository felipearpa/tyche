package com.felipearpa.tyche.pool.creator

import com.felipearpa.tyche.data.pool.domain.CreatePoolInput

internal fun CreatePoolModel.toCreatePoolInput(ownerGamblerId: String) =
    CreatePoolInput(
        poolLayoutId = poolLayoutId,
        poolName = poolName,
        ownerGamblerId = ownerGamblerId,
    )
