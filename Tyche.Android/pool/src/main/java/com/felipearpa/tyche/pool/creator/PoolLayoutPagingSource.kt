package com.felipearpa.tyche.pool.creator

import com.felipearpa.tyche.ui.paging.CursorPagingQuery
import com.felipearpa.tyche.ui.paging.CursorPagingSource

class PoolLayoutPagingSource(pagingQuery: CursorPagingQuery<PoolLayoutModel>) :
    CursorPagingSource<PoolLayoutModel>(pagingQuery)
