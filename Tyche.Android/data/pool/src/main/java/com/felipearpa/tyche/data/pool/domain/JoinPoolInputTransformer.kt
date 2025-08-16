package com.felipearpa.tyche.data.pool.domain

internal fun JoinPoolInput.toJoinPoolRequest() =
    JoinPoolRequest(
        gamblerId = gamblerId,
    )
