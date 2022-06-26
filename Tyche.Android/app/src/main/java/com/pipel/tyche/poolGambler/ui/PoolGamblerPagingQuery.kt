package com.pipel.tyche.poolGambler.ui

import com.pipel.core.CursorPage
import com.pipel.core.PagingQuery
import com.pipel.tyche.poolGambler.mapper.PoolGamblerMapper
import com.pipel.tyche.poolGambler.useCase.FindPoolsGamblersUseCase
import com.pipel.tyche.poolGambler.view.PoolGamblerModel

class PoolGamblerPagingQuery(
    private val findPoolsGamblersUseCase: FindPoolsGamblersUseCase,
    private val poolId: String,
    private val filterFunc: () -> String
) :
    PagingQuery<PoolGamblerModel> {

    override suspend fun execute(nextToken: String?): CursorPage<PoolGamblerModel> {
        return findPoolsGamblersUseCase.execute(poolId, nextToken, filterFunc())
            .map(PoolGamblerMapper::mapFromDomainToView)
    }

}