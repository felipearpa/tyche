package com.felipearpa.tyche.pool

import com.felipearpa.tyche.ui.paging.CursorPagingQuery
import com.felipearpa.tyche.ui.paging.CursorPagingSource

class PoolGamblerScorePagingSource(pagingQuery: CursorPagingQuery<PoolGamblerScoreModel>) :
    CursorPagingSource<PoolGamblerScoreModel>(pagingQuery)