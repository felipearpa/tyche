package com.felipearpa.tyche.pool.creator

import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.pool.application.GetOpenPoolLayoutsUseCase

suspend fun getOpenPoolLayoutsPagingQuery(
    next: String?,
    search: () -> String?,
    getOpenPoolLayoutsUseCase: GetOpenPoolLayoutsUseCase,
) = getOpenPoolLayoutsUseCase.execute(
    next = next,
    searchText = search(),
).map { page -> page.map { poolLayout -> poolLayout.toPoolLayoutModel() } }
