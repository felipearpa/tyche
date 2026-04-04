package com.felipearpa.tyche.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class PoolLayoutRemoteDataSource(private val httpClient: HttpClient) {
    suspend fun getOpenPoolLayouts(
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolLayoutResponse> =
        httpClient.get("pool-layouts/open") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()
}
