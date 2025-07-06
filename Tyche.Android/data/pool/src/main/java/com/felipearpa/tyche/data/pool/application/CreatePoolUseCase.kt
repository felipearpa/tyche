package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.CreatePoolInput
import com.felipearpa.tyche.data.pool.domain.PoolRepository
import jakarta.inject.Inject

class CreatePoolUseCase @Inject internal constructor(private val poolRepository: PoolRepository) {
    suspend fun execute(createPoolInput: CreatePoolInput) =
        poolRepository.createPool(createPoolInput = createPoolInput)
}
