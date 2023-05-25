package com.felipearpa.bet.ui

import com.felipearpa.ui.paging.CursorPagingQuery
import com.felipearpa.ui.paging.CursorPagingSource

class PoolGamblerBetPagingSource(pagingQuery: CursorPagingQuery<PoolGamblerBetModel>) :
    CursorPagingSource<PoolGamblerBetModel>(pagingQuery)