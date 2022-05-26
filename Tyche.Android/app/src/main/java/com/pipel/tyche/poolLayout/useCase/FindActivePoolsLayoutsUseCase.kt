package com.pipel.tyche.poolLayout.useCase

import com.pipel.core.CursorPage
import com.pipel.tyche.poolLayout.data.PoolLayoutRepository
import com.pipel.tyche.poolLayout.domain.PoolLayout
import com.pipel.tyche.poolLayout.mapper.PoolLayoutMapper
import javax.inject.Inject

interface FindActivePoolsLayoutsUseCase {

    suspend fun execute(nextToken: String?, filter: String = ""): CursorPage<PoolLayout>

}

class DefaultActiveFindActivePoolsLayoutsUseCase @Inject constructor(private val poolLayoutRepository: PoolLayoutRepository) :
    FindActivePoolsLayoutsUseCase {

    override suspend fun execute(nextToken: String?, filter: String): CursorPage<PoolLayout> {
        return poolLayoutRepository.getActivePoolsLayouts(nextToken, filter)
            .map(PoolLayoutMapper::mapFromDataToDomain)
    }

}