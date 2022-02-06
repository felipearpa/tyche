package com.pipel.tyche.poolLayout.ui

import com.pipel.core.CursorPage
import com.pipel.core.PagingQuery
import com.pipel.tyche.poolLayout.mapper.PoolLayoutMapper
import com.pipel.tyche.poolLayout.useCase.FindPoolsLayoutsUseCase
import com.pipel.tyche.poolLayout.view.PoolLayoutModel

class PoolLayoutPagingQuery(
    private val findPoolsLayoutsUseCase: FindPoolsLayoutsUseCase,
    private val filterFunc: () -> String
) :
    PagingQuery<PoolLayoutModel> {

    override suspend fun execute(nextToken: String?): CursorPage<PoolLayoutModel> {
        return findPoolsLayoutsUseCase.execute(nextToken, filterFunc())
            .map(PoolLayoutMapper::mapFromDomainToView)
    }

}