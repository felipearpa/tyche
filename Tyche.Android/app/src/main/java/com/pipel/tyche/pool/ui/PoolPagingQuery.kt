package com.pipel.tyche.pool.ui

import com.pipel.core.CursorPage
import com.pipel.core.PagingQuery
import com.pipel.tyche.pool.mapper.PoolMapper
import com.pipel.tyche.pool.useCase.FindPoolsUseCase
import com.pipel.tyche.pool.view.PoolModel

class PoolPagingQuery(
    private val findPoolsUseCase: FindPoolsUseCase,
    private val poolLayoutId: String,
    private val filterFunc: () -> String
) :
    PagingQuery<PoolModel> {

    override suspend fun execute(nextToken: String?): CursorPage<PoolModel> {
        return findPoolsUseCase.execute(poolLayoutId, nextToken, filterFunc())
            .map(PoolMapper::mapFromDomainToView)
    }

}