package com.felipearpa.pool.ui

import com.felipearpa.ui.paging.CursorPagingQuery
import com.felipearpa.ui.paging.CursorPagingSource

class PoolGamblerScorePagingSource(pagingQuery: CursorPagingQuery<PoolGamblerScoreModel>) :
    CursorPagingSource<PoolGamblerScoreModel>(pagingQuery)