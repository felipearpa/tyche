package com.pipel.tyche.poolLayout.useCase

import com.pipel.core.CursorPage
import com.pipel.tyche.poolLayout.data.PoolLayoutRepository
import com.pipel.tyche.poolLayout.domain.PoolLayout
import com.pipel.tyche.poolLayout.mapper.PoolLayoutMapper
import javax.inject.Inject

interface FindPoolsLayoutsUseCase {

    suspend fun execute(nextToken: String?, filter: String = ""): CursorPage<PoolLayout>

}

class DefaultFindPoolsLayoutsUseCase @Inject constructor(private val poolLayoutRepository: PoolLayoutRepository) :
    FindPoolsLayoutsUseCase {

    override suspend fun execute(nextToken: String?, filter: String): CursorPage<PoolLayout> {
        return poolLayoutRepository.getPoolsLayouts(nextToken, filter)
            .map(PoolLayoutMapper::mapFromDataToDomain)
    }

}