package com.felipearpa.tyche.data.bet.infrastructure

import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.bet.domain.BetRequest
import com.felipearpa.tyche.data.bet.domain.KickoffGroupFeedException
import com.felipearpa.tyche.data.bet.domain.KickoffGroupResponse
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetDataSource
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetResponse
import com.felipearpa.tyche.data.bet.domain.RankedMatchBetsException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    ): CursorPage<KickoffGroupResponse> =
        try {
            httpClient.get("pools/$poolId/gamblers/$gamblerId/bets/finished") {
                // Ktor omits a null parameter, so a first page sends no cursor.
                parameter("next", next)
            }.body()
        } catch (exception: ResponseException) {
            throw exception.toKickoffGroupFeedException() ?: exception
        }

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

    override suspend fun getRankedPoolMatchGamblerBets(
        poolId: String,
        matchId: String,
        next: String?,
    ): CursorPage<PoolGamblerBetResponse> =
        try {
            httpClient.get("pools/$poolId/matches/$matchId/bets") {
                parameter("sort", POOL_SCORE_DESCENDING_SORT)
                // Ktor omits a null parameter, so a first page sends no cursor.
                parameter("next", next)
            }.body()
        } catch (exception: ResponseException) {
            throw exception.toRankedMatchBetsException() ?: exception
        }

    override suspend fun getPoolGamblerBet(
        poolId: String,
        gamblerId: String,
        matchId: String,
    ): PoolGamblerBetResponse =
        httpClient.get("pools/$poolId/gamblers/$gamblerId/matches/$matchId").body()

    override suspend fun getGamblerBetsTimeline(
        poolId: String,
        gamblerId: String,
        next: String?,
    ): CursorPage<KickoffGroupResponse> =
        try {
            httpClient.get("pools/$poolId/gamblers/$gamblerId/bets/timeline") {
                parameter("next", next)
            }.body()
        } catch (exception: ResponseException) {
            throw exception.toKickoffGroupFeedException() ?: exception
        }

    override suspend fun bet(betRequest: BetRequest): PoolGamblerBetResponse =
        httpClient.patch("bets") {
            contentType(ContentType.Application.Json)
            setBody(betRequest)
        }.body()
}

/** The ranked match-bets order: total points in the pool, greatest first. */
private const val POOL_SCORE_DESCENDING_SORT = "pool-score-desc"

@Serializable
private data class ErrorResponse(val error: String? = null)

private val errorResponseJson = Json { ignoreUnknownKeys = true }

/** The response's `error` code, or `null` when the body is not the expected error JSON. */
private suspend fun ResponseException.errorCode(): String? =
    runCatching {
        errorResponseJson.decodeFromString<ErrorResponse>(response.bodyAsText()).error
    }.getOrNull()

/**
 * The ranked failure for this error response, or `null` when it is not one of the ranked mode's
 * own errors. A body that is not the expected JSON is not a ranked error.
 */
private suspend fun ResponseException.toRankedMatchBetsException(): RankedMatchBetsException? =
    RankedMatchBetsException.from(statusCode = response.status.value, errorCode = errorCode())

/** The grouped-feed failure for this error response, or `null` when it is not one of its own errors. */
private suspend fun ResponseException.toKickoffGroupFeedException(): KickoffGroupFeedException? =
    KickoffGroupFeedException.from(statusCode = response.status.value, errorCode = errorCode())
