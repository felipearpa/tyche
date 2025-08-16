package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.data.pool.domain.PoolLayout
import com.felipearpa.tyche.data.pool.domain.PoolLayoutRemoteDataSource
import com.felipearpa.tyche.data.pool.domain.PoolLayoutRepository
import com.felipearpa.tyche.data.pool.domain.toPoolLayout
import javax.inject.Inject

internal class PoolLayoutRemoteRepository @Inject constructor(
    private val poolLayoutRemoteDataSource: PoolLayoutRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler,
) : PoolLayoutRepository {
    override suspend fun getOpenPoolLayouts(
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolLayout>> {
        return networkExceptionHandler.handle {
            poolLayoutRemoteDataSource.getOpenPoolLayouts(next = next, searchText = searchText)
                .map { poolLayoutResponse -> poolLayoutResponse.toPoolLayout() }
        }
    }
}
