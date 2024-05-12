package com.felipearpa.tyche.bet.pending

import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.ui.paging.CursorPagingQuery
import com.felipearpa.tyche.ui.paging.CursorPagingSource

class PendingBetPagingSource(pagingQuery: CursorPagingQuery<PoolGamblerBetModel>) :
    CursorPagingSource<PoolGamblerBetModel>(pagingQuery)