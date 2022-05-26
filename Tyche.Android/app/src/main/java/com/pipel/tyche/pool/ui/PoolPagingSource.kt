package com.pipel.tyche.pool.ui

import com.pipel.core.PagingQuery
import com.pipel.tyche.pool.view.PoolModel
import com.pipel.ui.paging.QueryablePagingSource

class PoolPagingSource(query: PagingQuery<PoolModel>) :
    QueryablePagingSource<PoolModel>(query)