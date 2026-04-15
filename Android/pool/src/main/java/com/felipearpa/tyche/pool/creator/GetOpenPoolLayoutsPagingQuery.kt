package com.felipearpa.tyche.pool.creator

import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.pool.application.GetOpenPoolLayouts

suspend fun getOpenPoolLayoutsPagingQuery(
    next: String?,
    search: () -> String?,
    getOpenPoolLayouts: GetOpenPoolLayouts,
) = getOpenPoolLayouts.execute(
    next = next,
    searchText = search(),
).map { page -> page.map { poolLayout -> poolLayout.toPoolLayoutModel() } }
