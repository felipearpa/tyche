package com.pipel.tyche.ui.poollayout

import com.pipel.core.Page
import com.pipel.core.PagingQuery
import com.pipel.tyche.mapper.PoolLayoutMapper
import com.pipel.tyche.usecase.FindPoolsLayoutsUseCase

class PoolLayoutPagingQuery(
    private val findPoolsLayoutsUseCase: FindPoolsLayoutsUseCase,
    private val filterFunc: () -> String
) :
    PagingQuery<PoolLayoutModel> {

    override suspend fun execute(skip: Int, take: Int): Page<PoolLayoutModel> {
        return findPoolsLayoutsUseCase.execute(skip, take, filterFunc())
            .map(PoolLayoutMapper::mapFromDomainToView)
    }

}