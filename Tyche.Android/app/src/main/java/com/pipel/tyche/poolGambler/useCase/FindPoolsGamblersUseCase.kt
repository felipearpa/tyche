package com.pipel.tyche.poolGambler.useCase

import com.pipel.core.CursorPage
import com.pipel.tyche.poolGambler.data.PoolGamblerRepository
import com.pipel.tyche.poolGambler.domain.PoolGambler
import com.pipel.tyche.poolGambler.mapper.PoolGamblerMapper
import javax.inject.Inject

interface FindPoolsGamblersUseCase {

    suspend fun execute(
        poolId: String,
        nextToken: String?,
        filter: String?
    ): CursorPage<PoolGambler>

}

class DefaultFindPoolsGamblersUseCase @Inject constructor(private val poolGamblerRepository: PoolGamblerRepository) :
    FindPoolsGamblersUseCase {

    override suspend fun execute(
        poolId: String,
        nextToken: String?,
        filter: String?
    ): CursorPage<PoolGambler> {
        return poolGamblerRepository.getPoolsGamblers(poolId, nextToken, filter)
            .map(PoolGamblerMapper::mapFromDataToDomain)
    }

}