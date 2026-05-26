package com.felipearpa.tyche.pool.managegamblers

import com.felipearpa.tyche.ui.paging.CursorPagingQuery
import com.felipearpa.tyche.ui.paging.CursorPagingSource

class PoolMemberPagingSource(pagingQuery: CursorPagingQuery<PoolMemberModel>) :
    CursorPagingSource<PoolMemberModel>(pagingQuery)
