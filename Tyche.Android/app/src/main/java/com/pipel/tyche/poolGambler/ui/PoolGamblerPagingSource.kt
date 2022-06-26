package com.pipel.tyche.poolGambler.ui

import com.pipel.core.PagingQuery
import com.pipel.tyche.poolGambler.view.PoolGamblerModel
import com.pipel.ui.paging.QueryablePagingSource

class PoolGamblerPagingSource(query: PagingQuery<PoolGamblerModel>) :
    QueryablePagingSource<PoolGamblerModel>(query)