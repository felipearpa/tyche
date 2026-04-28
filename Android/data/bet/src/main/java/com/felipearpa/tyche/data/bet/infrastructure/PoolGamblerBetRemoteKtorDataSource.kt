package com.felipearpa.tyche.data.bet.infrastructure

import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.bet.domain.BetRequest
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetDataSource
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class PoolGamblerBetRemoteKtorDataSource(private val httpClient: HttpClient) :
    PoolGamblerBetDataSource {
    override suspend fun getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): CursorPage<PoolGamblerBetResponse> =
        httpClient.get("pools/$poolId/gamblers/$gamblerId/bets/pending") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()

    override suspend fun getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): CursorPage<PoolGamblerBetResponse> =
        httpClient.get("pools/$poolId/gamblers/$gamblerId/bets/finished") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()

    override suspend fun getLivePoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): CursorPage<PoolGamblerBetResponse> =
        httpClient.get("pools/$poolId/gamblers/$gamblerId/bets/live") {
            parameter("next", next)
            parameter("searchText", searchText)
        }.body()

    override suspend fun bet(betRequest: BetRequest): PoolGamblerBetResponse =
        httpClient.patch("bets") {
            contentType(ContentType.Application.Json)
            setBody(betRequest)
        }.body()
}
