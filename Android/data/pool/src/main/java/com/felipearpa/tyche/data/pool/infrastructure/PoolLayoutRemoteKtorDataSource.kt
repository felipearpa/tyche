package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.pool.domain.PoolLayoutDataSource
import com.felipearpa.tyche.data.pool.domain.PoolLayoutResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class PoolLayoutRemoteKtorDataSource(private val httpClient: HttpClient) :
    PoolLayoutDataSource {
    override suspend fun getOpenPoolLayouts(
        next: String?,
        searchText: String?,
    ): CursorPage<PoolLayoutResponse> =
        httpClient.get("pool-layouts/open") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()
}
