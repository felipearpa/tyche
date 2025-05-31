package com.felipearpa.tyche.data.pool.application

import com.felipearpa.tyche.data.pool.domain.PoolLayoutRepository
import javax.inject.Inject

class GetOpenPoolLayoutsUseCase @Inject internal constructor(private val poolLayoutRepository: PoolLayoutRepository) {
    suspend fun execute() =
        poolLayoutRepository.getOpenPoolLayouts()
}
