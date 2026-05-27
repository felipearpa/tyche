package com.felipearpa.tyche.pool.managegamblers

import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.pool.application.GetPoolMembers

suspend fun getPoolMembersPagingQuery(
    next: String?,
    poolId: String,
    getPoolMembers: GetPoolMembers,
) =
    getPoolMembers.execute(poolId = poolId, next = next)
        .map { page -> page.map { member -> member.toPoolMemberModel() } }
