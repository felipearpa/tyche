package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.PoolMemberRepository

class RemoveGambler internal constructor(private val poolMemberRepository: PoolMemberRepository) {
    suspend fun execute(poolId: String, gamblerId: String) =
        poolMemberRepository.removeGambler(poolId = poolId, gamblerId = gamblerId)
}
