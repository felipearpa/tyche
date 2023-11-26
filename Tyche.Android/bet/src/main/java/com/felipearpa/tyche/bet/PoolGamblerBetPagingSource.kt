package com.felipearpa.tyche.bet

import com.felipearpa.tyche.ui.paging.CursorPagingQuery
import com.felipearpa.tyche.ui.paging.CursorPagingSource

class PoolGamblerBetPagingSource(pagingQuery: CursorPagingQuery<PoolGamblerBetModel>) :
    CursorPagingSource<PoolGamblerBetModel>(pagingQuery)