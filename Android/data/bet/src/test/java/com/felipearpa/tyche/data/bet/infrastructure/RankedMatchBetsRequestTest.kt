package com.felipearpa.tyche.data.bet.infrastructure

import com.felipearpa.network.HttpStatus
import com.felipearpa.network.NetworkException
import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.network.serializer.LocalDateTimeSerializer
import com.felipearpa.tyche.data.bet.domain.RankedMatchBetsException
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test

class RankedMatchBetsRequestTest {

    @Test
    fun `given a first page when the ranked bets are requested then the request asks for pool order without a cursor`() =
        runTest {
            val requests = mutableListOf<HttpRequestData>()
            val repository = repository { request ->
                requests += request
                respondJson(HttpStatusCode.OK, PAGE_BODY)
            }

            val page = repository.getRankedPoolMatchGamblerBets(poolId = "pool-1", matchId = "match-1")
                .getOrThrow()

            val url = requests.single().url
            url.encodedPath shouldBe "/pools/pool-1/matches/match-1/bets"
            url.parameters.names() shouldBe setOf("sort")
            url.parameters["sort"] shouldBe "pool-score-desc"
            page.items.map { it.gamblerId } shouldContainExactly listOf("leader", "second")
            page.next shouldBe "cursor-2"
        }

    @Test
    fun `given a cursor when the next page is requested then the cursor is sent with the ranked order`() =
        runTest {
            val requests = mutableListOf<HttpRequestData>()
            val repository = repository { request ->
                requests += request
                respondJson(HttpStatusCode.OK, """{"items":[],"next":null}""")
            }

            val page = repository.getRankedPoolMatchGamblerBets(poolId = "pool-1", matchId = "match-1", next = "abc")
                .getOrThrow()

            requests.single().url.parameters["sort"] shouldBe "pool-score-desc"
            requests.single().url.parameters["next"] shouldBe "abc"
            page.next.shouldBeNull()
        }

    @Test
    fun `given predictions are hidden when the ranked bets are requested then the failure says so`() = runTest {
        val result = repository { respondError(HttpStatusCode.Conflict, "PREDICTIONS_HIDDEN") }
            .getRankedPoolMatchGamblerBets(poolId = "pool-1", matchId = "match-1")

        result.exceptionOrNull().shouldBeInstanceOf<RankedMatchBetsException.PredictionsHidden>()
    }

    @Test
    fun `given the ranking is not ready or unreadable when the ranked bets are requested then the failure is recoverable`() =
        runTest {
            listOf("RANKING_NOT_READY", "RANKING_UNAVAILABLE").forEach { code ->
                val result = repository { respondError(HttpStatusCode.ServiceUnavailable, code) }
                    .getRankedPoolMatchGamblerBets(poolId = "pool-1", matchId = "match-1")

                result.exceptionOrNull().shouldBeInstanceOf<RankedMatchBetsException.RankingUnavailable>()
            }
        }

    @Test
    fun `given a rejected cursor when the next page is requested then the failure asks for a restart`() = runTest {
        listOf("INVALID_CURSOR", "CURSOR_CONTEXT_MISMATCH").forEach { code ->
            val result = repository { respondError(HttpStatusCode.BadRequest, code) }
                .getRankedPoolMatchGamblerBets(poolId = "pool-1", matchId = "match-1", next = "stale")

            result.exceptionOrNull().shouldBeInstanceOf<RankedMatchBetsException.CursorRejected>()
        }
    }

    @Test
    fun `given another error response when the ranked bets are requested then the usual network failure remains`() =
        runTest {
            val forbidden = repository { respondError(HttpStatusCode.Forbidden, "NOT_POOL_MEMBER") }
                .getRankedPoolMatchGamblerBets(poolId = "pool-1", matchId = "match-1")
            val unexpectedBody = repository {
                respond(content = "Service Unavailable", status = HttpStatusCode.ServiceUnavailable)
            }.getRankedPoolMatchGamblerBets(poolId = "pool-1", matchId = "match-1")

            forbidden.exceptionOrNull() shouldBe NetworkException.Http(HttpStatus.FORBIDDEN)
            unexpectedBody.exceptionOrNull() shouldBe NetworkException.Http(HttpStatus.SERVICE_UNAVAILABLE)
        }

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): PoolGamblerBetRemoteRepository {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        serializersModule = SerializersModule {
                            contextual(LocalDateTime::class, LocalDateTimeSerializer)
                        }
                    },
                )
            }
            defaultRequest { url("https://api.example.com/") }
        }
        return PoolGamblerBetRemoteRepository(
            poolGamblerBetDataSource = PoolGamblerBetRemoteKtorDataSource(httpClient = client),
            networkExceptionHandler = ResponseStatusExceptionHandler,
        )
    }

    private fun MockRequestHandleScope.respondJson(status: HttpStatusCode, body: String) =
        respond(
            content = body,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )

    private fun MockRequestHandleScope.respondError(status: HttpStatusCode, code: String) =
        respondJson(status, """{"error":"$code","message":"irrelevant"}""")

    /** Mirrors the app's Ktor handler: an error response becomes `NetworkException.Http`. */
    private object ResponseStatusExceptionHandler : NetworkExceptionHandler {
        override suspend fun <Value> handle(block: suspend () -> Value): Result<Value> =
            try {
                Result.success(block())
            } catch (exception: ResponseException) {
                Result.failure(NetworkException.Http(HttpStatus(exception.response.status.value)))
            } catch (exception: Exception) {
                Result.failure(exception)
            }
    }
}

private const val PAGE_BODY = """
{
  "items": [
    {"poolId":"pool-1","gamblerId":"leader","gamblerUsername":"El mono","matchId":"match-1",
     "homeTeamId":"es","homeTeamName":"España","homeTeamScore":0,"homeTeamBet":null,
     "awayTeamId":"ar","awayTeamName":"Argentina","awayTeamScore":0,"awayTeamBet":null,
     "score":0,"matchDateTime":"2026-07-19T19:00:00Z","isLocked":true,"isComputed":true},
    {"poolId":"pool-1","gamblerId":"second","gamblerUsername":"Juano","matchId":"match-1",
     "homeTeamId":"es","homeTeamName":"España","homeTeamScore":0,"homeTeamBet":1,
     "awayTeamId":"ar","awayTeamName":"Argentina","awayTeamScore":0,"awayTeamBet":0,
     "score":4,"matchDateTime":"2026-07-19T19:00:00Z","isLocked":true,"isComputed":true}
  ],
  "next": "cursor-2"
}
"""
