package com.pipel.tyche.poolLayout.ui

import com.pipel.core.CursorPage
import com.pipel.core.PagingQuery
import com.pipel.tyche.poolLayout.mapper.PoolLayoutMapper
import com.pipel.tyche.poolLayout.useCase.FindActivePoolsLayoutsUseCase
import com.pipel.tyche.poolLayout.view.PoolLayoutModel

class PoolLayoutPagingQuery(
    private val findActivePoolsLayoutsUseCase: FindActivePoolsLayoutsUseCase,
    private val filterFunc: () -> String
) :
    PagingQuery<PoolLayoutModel> {

    override suspend fun execute(nextToken: String?): CursorPage<PoolLayoutModel> {
        return findActivePoolsLayoutsUseCase.execute(nextToken, filterFunc())
            .map(PoolLayoutMapper::mapFromDomainToView)
    }

}