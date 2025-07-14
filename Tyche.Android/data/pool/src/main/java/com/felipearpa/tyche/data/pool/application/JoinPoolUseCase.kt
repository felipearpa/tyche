package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.JoinPoolInput
import com.felipearpa.tyche.data.pool.domain.PoolRepository
import jakarta.inject.Inject

class JoinPoolUseCase @Inject internal constructor(private val poolRepository: PoolRepository) {
    suspend fun execute(joinPoolInput: JoinPoolInput) =
        poolRepository.joinPool(joinPoolInput = joinPoolInput)
}
