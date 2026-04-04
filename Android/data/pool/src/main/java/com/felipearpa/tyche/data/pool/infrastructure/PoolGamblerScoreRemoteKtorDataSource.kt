package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreDataSource
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScoreResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class PoolGamblerScoreRemoteKtorDataSource(private val httpClient: HttpClient) :
    PoolGamblerScoreDataSource {
    override suspend fun getPoolGamblerScoresByGambler(
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): CursorPage<PoolGamblerScoreResponse> =
        httpClient.get("gamblers/$gamblerId/pools") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()

    override suspend fun getPoolGamblerScoresByPool(
        poolId: String,
        next: String?,
        searchText: String?,
    ): CursorPage<PoolGamblerScoreResponse> =
        httpClient.get("pools/$poolId/gamblers") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()

    override suspend fun getPoolGamblerScore(
        poolId: String,
        gamblerId: String,
    ): PoolGamblerScoreResponse =
        httpClient.get("pools/$poolId/gamblers/$gamblerId").body()
}
