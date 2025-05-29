package com.felipearpa.tyche.poollayout.infrastructure

import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.poollayout.domain.PoolLayout
import com.felipearpa.tyche.poollayout.domain.PoolLayoutRemoteDataSource
import com.felipearpa.tyche.poollayout.domain.PoolLayoutRepository
import com.felipearpa.tyche.poollayout.domain.toPoolLayout
import javax.inject.Inject

internal class PoolLayoutRemoteRepository @Inject constructor(
    private val poolLayoutRemoteDataSource: PoolLayoutRemoteDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler
) : PoolLayoutRepository {
    override suspend fun getOpenPoolLayouts(): Result<CursorPage<PoolLayout>> {
        return networkExceptionHandler.handle {
            poolLayoutRemoteDataSource.getOpenPoolLayouts()
                .map { poolLayoutResponse -> poolLayoutResponse.toPoolLayout() }
        }
    }
}
