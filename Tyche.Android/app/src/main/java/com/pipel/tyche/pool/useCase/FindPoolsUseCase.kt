package com.pipel.tyche.pool.useCase

import com.pipel.core.CursorPage
import com.pipel.tyche.pool.data.PoolRepository
import com.pipel.tyche.pool.domain.Pool
import com.pipel.tyche.pool.mapper.PoolMapper
import javax.inject.Inject

interface FindPoolsUseCase {

    suspend fun execute(
        poolLayoutId: String,
        nextToken: String?,
        filter: String?
    ): CursorPage<Pool>

}

class DefaultFindPoolsUseCase @Inject constructor(private val poolRepository: PoolRepository) :
    FindPoolsUseCase {

    override suspend fun execute(
        poolLayoutId: String,
        nextToken: String?,
        filter: String?
    ): CursorPage<Pool> {
        return poolRepository.getPools(poolLayoutId, nextToken, filter)
            .map(PoolMapper::mapFromDataToDomain)
    }

}