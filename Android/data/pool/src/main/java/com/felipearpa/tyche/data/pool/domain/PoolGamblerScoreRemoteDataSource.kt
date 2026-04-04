package com.felipearpa.tyche.data.pool.domain

import com.felipearpa.tyche.core.paging.CursorPage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class PoolGamblerScoreRemoteDataSource(private val httpClient: HttpClient) {
    suspend fun getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerScoreResponse> =
        httpClient.get("gamblers/$gamblerId/pools") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()

    suspend fun getPoolGamblerScoresByPool(
        poolId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerScoreResponse> =
        httpClient.get("pools/$poolId/gamblers") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()

    suspend fun getPoolGamblerScore(
        poolId: String,
        gamblerId: String,
    ): PoolGamblerScoreResponse =
        httpClient.get("pools/$poolId/gamblers/$gamblerId").body()
}
