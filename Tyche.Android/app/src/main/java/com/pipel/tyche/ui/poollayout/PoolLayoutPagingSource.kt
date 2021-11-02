package com.pipel.tyche.ui.poollayout

import com.pipel.core.PagingQuery
import com.pipel.ui.paging.QueryablePagingSource

class PoolLayoutPagingSource(query: PagingQuery<PoolLayoutModel>) :
    QueryablePagingSource<PoolLayoutModel>(query)