package com.felipearpa.tyche.data.bet.domain

import com.felipearpa.tyche.core.paging.CursorPage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class PoolGamblerBetRemoteDataSource(private val httpClient: HttpClient) {
    suspend fun getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerBetResponse> =
        httpClient.get("pools/$poolId/gamblers/$gamblerId/bets/pending") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()

    suspend fun getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String? = null,
        searchText: String? = null,
    ): CursorPage<PoolGamblerBetResponse> =
        httpClient.get("pools/$poolId/gamblers/$gamblerId/bets/finished") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()

    suspend fun bet(betRequest: BetRequest): PoolGamblerBetResponse =
        httpClient.patch("bets") {
            contentType(ContentType.Application.Json)
            setBody(betRequest)
        }.body()
}
