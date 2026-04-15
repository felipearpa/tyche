package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.JoinPoolInput
import com.felipearpa.tyche.data.pool.domain.PoolRepository

class JoinPool internal constructor(private val poolRepository: PoolRepository) {
    suspend fun execute(joinPoolInput: JoinPoolInput) =
        poolRepository.joinPool(joinPoolInput = joinPoolInput)
}
