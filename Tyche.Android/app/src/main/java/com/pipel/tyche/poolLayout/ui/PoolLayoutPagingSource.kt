package com.pipel.tyche.poolLayout.ui

import com.pipel.core.PagingQuery
import com.pipel.tyche.poolLayout.view.PoolLayoutModel
import com.pipel.ui.paging.QueryablePagingSource

class PoolLayoutPagingSource(query: PagingQuery<PoolLayoutModel>) :
    QueryablePagingSource<PoolLayoutModel>(query)