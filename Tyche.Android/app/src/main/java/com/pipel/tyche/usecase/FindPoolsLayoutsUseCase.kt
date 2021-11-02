package com.pipel.tyche.usecase

import com.pipel.core.Page
import com.pipel.tyche.data.PoolLayoutRepository
import com.pipel.tyche.domain.PoolLayout
import com.pipel.tyche.mapper.PoolLayoutMapper
import javax.inject.Inject

interface FindPoolsLayoutsUseCase {

    suspend fun execute(skip: Int, take: Int, filter: String = ""): Page<PoolLayout>

}

class DefaultFindPoolsLayoutsUseCase @Inject constructor(private val poolLayoutRepository: PoolLayoutRepository) :
    FindPoolsLayoutsUseCase {

    override suspend fun execute(
        skip: Int,
        take: Int,
        filter: String
    ): Page<PoolLayout> {
        return poolLayoutRepository.getPoolsLayouts(skip, take, filter)
            .map(PoolLayoutMapper::mapFromDataToDomain)
    }

}