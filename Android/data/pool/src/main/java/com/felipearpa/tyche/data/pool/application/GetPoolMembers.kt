package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.PoolMemberRepository

class GetPoolMembers internal constructor(private val poolMemberRepository: PoolMemberRepository) {
    suspend fun execute(poolId: String, next: String? = null) =
        poolMemberRepository.getPoolMembers(poolId = poolId, next = next)
}
