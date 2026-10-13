package com.felipearpa.tyche.data.bet.infrastructure

import com.felipearpa.network.HttpStatus
import com.felipearpa.network.NetworkException
import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.network.serializer.LocalDateTimeSerializer
import com.felipearpa.tyche.data.bet.domain.GroupMovement
import com.felipearpa.tyche.data.bet.domain.KickoffGroup
import com.felipearpa.tyche.data.bet.domain.KickoffGroupFeedException
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test
import kotlin.time.Instant

/**
 * The grouped History and Timeline contract: whole kickoff groups identified by their UTC instant,
 * the server's counts and movement, and a rejected continuation reported as a restart.
 */
class KickoffGroupFeedRequestTest {

    @Test
    fun `given a first History page when it is requested then no cursor or search text is sent`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repository { request ->
            requests += request
            respondJson(HttpStatusCode.OK, """{"items":[],"next":null}""")
        }

        repository.getFinishedPoolGamblerBets(poolId = "pool-1", gamblerId = "gambler-1").getOrThrow()

        val url = requests.single().url
        url.encodedPath shouldBe "/pools/pool-1/gamblers/gambler-1/bets/finished"
        url.parameters.names() shouldBe emptySet()
    }

    @Test
    fun `given a Timeline cursor when the next page is requested then the cursor is sent`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repository { request ->
            requests += request
            respondJson(HttpStatusCode.OK, """{"items":[],"next":null}""")
        }

        repository.getGamblerBetsTimeline(poolId = "pool-1", gamblerId = "gambler-1", next = "live:abc").getOrThrow()

        val url = requests.single().url
        url.encodedPath shouldBe "/pools/pool-1/gamblers/gambler-1/bets/timeline"
        url.parameters["next"] shouldBe "live:abc"
    }

    @Test
    fun `given a grouped page when it is decoded then each group keeps the server's instant, counts, movement, and rows`() =
        runTest {
            val page = repository { respondJson(HttpStatusCode.OK, GROUPED_PAGE) }
                .getFinishedPoolGamblerBets(poolId = "pool-1", gamblerId = "gambler-1")
                .getOrThrow()

            page.next shouldBe "cursor-2"
            val (complete, partial) = page.items
            complete.kickoffDateTime shouldBe Instant.parse("2026-06-12T17:00:00Z")
            complete.totalMatches shouldBe 2
            complete.computedMatches shouldBe 2
            complete.movement shouldBe GroupMovement(beforePosition = 8, position = 6)
            complete.matches.map { it.matchId } shouldContainExactly listOf("match-a", "match-b")

            partial.totalMatches shouldBe 4
            partial.computedMatches shouldBe 2
            partial.movement.shouldBeNull()
            partial.matches.single().isComputed shouldBe true
        }

    @Test
    fun `given a group without computed matches when it is decoded then its movement is null and its rows are kept`() =
        runTest {
            val page = repository {
                respondJson(
                    HttpStatusCode.OK,
                    pageOf(group("2026-06-13T20:00:00Z", computed = 0, movement = "null", isComputed = false)),
                )
            }.getGamblerBetsTimeline(poolId = "pool-1", gamblerId = "gambler-1").getOrThrow()

            val group = page.items.single()
            group.computedMatches shouldBe 0
            group.movement.shouldBeNull()
            group.matches.single().isComputed shouldBe false
        }

    @Test
    fun `given two instants with the same local label when they are decoded then they stay two groups`() = runTest {
        // New York repeats 01:30 when daylight saving time ends on 2026-11-01: 05:30Z and 06:30Z
        // both read 01:30 there.
        val page = repository {
            respondJson(
                HttpStatusCode.OK,
                pageOf(
                    group("2026-11-01T06:30:00Z", computed = 1, movement = """{"beforePosition":3,"position":2}"""),
                    group("2026-11-01T05:30:00Z", computed = 1, movement = """{"beforePosition":4,"position":3}"""),
                ),
            )
        }.getFinishedPoolGamblerBets(poolId = "pool-1", gamblerId = "gambler-1").getOrThrow()

        val newYork = TimeZone.of("America/New_York")
        val (later, earlier) = page.items.map(KickoffGroup::kickoffDateTime)
        later.toLocalDateTime(newYork) shouldBe earlier.toLocalDateTime(newYork)
        later.toLocalDateTime(newYork) shouldBe LocalDateTime(2026, 11, 1, 1, 30)
        later shouldNotBe earlier
        page.items.map(KickoffGroup::kickoffDateTime).toSet().size shouldBe 2
    }

    @Test
    fun `given the same instant written with another offset when it is decoded then it is the same kickoff`() =
        runTest {
            val page = repository {
                respondJson(HttpStatusCode.OK, pageOf(group("2026-06-12T12:00:00-05:00", computed = 1, movement = "null")))
            }.getFinishedPoolGamblerBets(poolId = "pool-1", gamblerId = "gambler-1").getOrThrow()

            page.items.single().kickoffDateTime shouldBe Instant.parse("2026-06-12T17:00:00Z")
        }

    @Test
    fun `given a kickoff that is not an instant when the page is decoded then the request fails`() = runTest {
        val result = repository {
            respondJson(HttpStatusCode.OK, pageOf(group("2026-06-12 17:00", computed = 1, movement = "null", rowKickoff = "2026-06-12T17:00:00Z")))
        }.getFinishedPoolGamblerBets(poolId = "pool-1", gamblerId = "gambler-1")

        result.isFailure shouldBe true
    }

    @Test
    fun `given a rejected cursor when either feed appends then the failure asks for a restart`() = runTest {
        listOf("INVALID_CURSOR", "CURSOR_CONTEXT_MISMATCH").forEach { code ->
            val history = repository { respondError(HttpStatusCode.BadRequest, code) }
                .getFinishedPoolGamblerBets(poolId = "pool-1", gamblerId = "gambler-1", next = "stale")
            val timeline = repository { respondError(HttpStatusCode.BadRequest, code) }
                .getGamblerBetsTimeline(poolId = "pool-1", gamblerId = "gambler-1", next = "finished:stale")

            history.exceptionOrNull().shouldBeInstanceOf<KickoffGroupFeedException.CursorRejected>()
            timeline.exceptionOrNull().shouldBeInstanceOf<KickoffGroupFeedException.CursorRejected>()
        }
    }

    @Test
    fun `given another error response when a feed is requested then the usual network failure remains`() = runTest {
        val badRequest = repository { respondError(HttpStatusCode.BadRequest, "SOMETHING_ELSE") }
            .getFinishedPoolGamblerBets(poolId = "pool-1", gamblerId = "gambler-1")
        val unexpectedBody = repository { respond(content = "Bad Gateway", status = HttpStatusCode.BadGateway) }
            .getGamblerBetsTimeline(poolId = "pool-1", gamblerId = "gambler-1")

        badRequest.exceptionOrNull() shouldBe NetworkException.Http(HttpStatus.BAD_REQUEST)
        unexpectedBody.exceptionOrNull() shouldBe NetworkException.Http(HttpStatus.BAD_GATEWAY)
    }

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
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

private fun pageOf(vararg groups: String) = """{"items":[${groups.joinToString(",")}],"next":null}"""

private fun group(
    kickoff: String,
    computed: Int,
    movement: String,
    isComputed: Boolean = true,
    rowKickoff: String = kickoff,
) = """
{"kickoffDateTime":"$kickoff","totalMatches":2,"computedMatches":$computed,"movement":$movement,
 "matches":[${row("match-$kickoff", rowKickoff, isComputed)}]}
"""

private fun row(matchId: String, kickoff: String, isComputed: Boolean) = """
{"poolId":"pool-1","gamblerId":"gambler-1","gamblerUsername":"El mono","matchId":"$matchId",
 "homeTeamId":"es","homeTeamName":"España","homeTeamScore":${if (isComputed) 1 else null},"homeTeamBet":2,
 "awayTeamId":"ar","awayTeamName":"Argentina","awayTeamScore":${if (isComputed) 0 else null},"awayTeamBet":1,
 "score":${if (isComputed) 2 else null},"matchDateTime":"$kickoff","isLocked":true,"isComputed":$isComputed}
"""

private val GROUPED_PAGE = """
{
  "items": [
    {"kickoffDateTime":"2026-06-12T17:00:00Z","totalMatches":2,"computedMatches":2,
     "movement":{"beforePosition":8,"position":6},
     "matches":[${row("match-a", "2026-06-12T17:00:00Z", true)},${row("match-b", "2026-06-12T17:00:00Z", true)}]},
    {"kickoffDateTime":"2026-06-11T20:00:00Z","totalMatches":4,"computedMatches":2,"movement":null,
     "matches":[${row("match-c", "2026-06-11T20:00:00Z", true)}]}
  ],
  "next": "cursor-2"
}
"""
