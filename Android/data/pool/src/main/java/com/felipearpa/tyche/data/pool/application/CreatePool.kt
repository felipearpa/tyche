package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.CreatePoolInput
import com.felipearpa.tyche.data.pool.domain.PoolRepository

class CreatePool internal constructor(private val poolRepository: PoolRepository) {
    suspend fun execute(createPoolInput: CreatePoolInput) =
        poolRepository.createPool(createPoolInput = createPoolInput)
}
