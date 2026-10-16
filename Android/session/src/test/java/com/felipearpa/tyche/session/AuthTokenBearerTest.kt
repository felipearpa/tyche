package com.felipearpa.tyche.session

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException

class AuthTokenBearerTest {

    @Test
    fun `given an expired token when a request gets a 401 without a challenge header then one forced refresh happens and the request is retried once with the new token`() =
        runTest {
            val retriever = FakeAuthTokenRetriever(currentToken = STALE_TOKEN) { FRESH_TOKEN }
            val authorizations = mutableListOf<String?>()
            val client = client(retriever) { request ->
                authorizations += request.headers[HttpHeaders.Authorization]
                if (request.headers[HttpHeaders.Authorization] == "Bearer $FRESH_TOKEN") ok() else unauthorized()
            }

            val response = client.get(URL)

            response.status shouldBe HttpStatusCode.OK
            authorizations shouldContainExactly listOf("Bearer $STALE_TOKEN", "Bearer $FRESH_TOKEN")
            retriever.forcedRefreshCount shouldBe 1
        }

    @Test
    fun `given an expired token when a request gets a 401 with a bearer challenge then one forced refresh happens and the request is retried once with the new token`() =
        runTest {
            val retriever = FakeAuthTokenRetriever(currentToken = STALE_TOKEN) { FRESH_TOKEN }
            val authorizations = mutableListOf<String?>()
            val client = client(retriever) { request ->
                authorizations += request.headers[HttpHeaders.Authorization]
                if (request.headers[HttpHeaders.Authorization] == "Bearer $FRESH_TOKEN") {
                    ok()
                } else {
                    unauthorized(headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
                }
            }

            val response = client.get(URL)

            response.status shouldBe HttpStatusCode.OK
            authorizations shouldContainExactly listOf("Bearer $STALE_TOKEN", "Bearer $FRESH_TOKEN")
            retriever.forcedRefreshCount shouldBe 1
        }

    @Test
    fun `given a retried request when it gets a 401 again then it is not retried further and the 401 surfaces`() =
        runTest {
            val retriever = FakeAuthTokenRetriever(currentToken = STALE_TOKEN) { FRESH_TOKEN }
            val authorizations = mutableListOf<String?>()
            val client = client(retriever) { request ->
                authorizations += request.headers[HttpHeaders.Authorization]
                unauthorized()
            }

            val exception = shouldThrow<ClientRequestException> { client.get(URL) }

            exception.response.status shouldBe HttpStatusCode.Unauthorized
            authorizations shouldContainExactly listOf("Bearer $STALE_TOKEN", "Bearer $FRESH_TOKEN")
            retriever.forcedRefreshCount shouldBe 1
        }

    @Test
    fun `given a forced refresh that fails when a request gets a 401 then the request is not retried and the original 401 surfaces`() =
        runTest {
            val retriever = FakeAuthTokenRetriever(currentToken = STALE_TOKEN) {
                throw IOException("token endpoint unreachable")
            }
            val authorizations = mutableListOf<String?>()
            val client = client(retriever) { request ->
                authorizations += request.headers[HttpHeaders.Authorization]
                unauthorized()
            }

            val exception = shouldThrow<ClientRequestException> { client.get(URL) }

            exception.response.status shouldBe HttpStatusCode.Unauthorized
            authorizations shouldContainExactly listOf("Bearer $STALE_TOKEN")
            retriever.forcedRefreshCount shouldBe 1
        }

    @Test
    fun `given a forced refresh that yields no token when a request gets a 401 then the request is not retried and the original 401 surfaces`() =
        runTest {
            val retriever = FakeAuthTokenRetriever(currentToken = STALE_TOKEN) { null }
            val authorizations = mutableListOf<String?>()
            val client = client(retriever) { request ->
                authorizations += request.headers[HttpHeaders.Authorization]
                unauthorized()
            }

            val exception = shouldThrow<ClientRequestException> { client.get(URL) }

            exception.response.status shouldBe HttpStatusCode.Unauthorized
            authorizations shouldContainExactly listOf("Bearer $STALE_TOKEN")
            retriever.forcedRefreshCount shouldBe 1
        }

    @Test
    fun `given ordinary requests when the retriever's current token changes then each request carries the current non-forced token without a 401`() =
        runTest {
            val retriever = FakeAuthTokenRetriever(currentToken = STALE_TOKEN) { FRESH_TOKEN }
            val authorizations = mutableListOf<String?>()
            val client = client(retriever) { request ->
                authorizations += request.headers[HttpHeaders.Authorization]
                ok()
            }

            client.get(URL).status shouldBe HttpStatusCode.OK
            retriever.currentToken = ROTATED_TOKEN
            client.get(URL).status shouldBe HttpStatusCode.OK

            authorizations shouldContainExactly listOf("Bearer $STALE_TOKEN", "Bearer $ROTATED_TOKEN")
            retriever.forcedRefreshCount shouldBe 0
        }

    @Test
    fun `given no signed-in user when a request is sent then it goes out without an authorization header`() =
        runTest {
            val retriever = FakeAuthTokenRetriever(currentToken = null) { null }
            val authorizations = mutableListOf<String?>()
            val client = client(retriever) { request ->
                authorizations += request.headers[HttpHeaders.Authorization]
                ok()
            }

            client.get(URL).status shouldBe HttpStatusCode.OK

            authorizations shouldContainExactly listOf(null)
            retriever.forcedRefreshCount shouldBe 0
        }
}

private const val URL = "https://api.example.test/pools"
private const val STALE_TOKEN = "stale-token"
private const val FRESH_TOKEN = "fresh-token"
private const val ROTATED_TOKEN = "rotated-token"

private fun client(
    authTokenRetriever: AuthTokenRetriever,
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) = HttpClient(MockEngine(handler)) {
    expectSuccess = true
    install(Auth) {
        authTokenBearer(authTokenRetriever)
    }
}

private fun MockRequestHandleScope.ok() = respond(content = "", status = HttpStatusCode.OK)

private fun MockRequestHandleScope.unauthorized(headers: Headers = headersOf()) =
    respond(content = "", status = HttpStatusCode.Unauthorized, headers = headers)

/**
 * Mirrors Firebase: an ordinary load returns the current token, and a successful forced refresh
 * replaces the current token with the new one.
 */
private class FakeAuthTokenRetriever(
    var currentToken: String?,
    private val forcedRefresh: () -> String?,
) : AuthTokenRetriever {
    var forcedRefreshCount = 0
        private set

    override suspend fun authToken(forceRefresh: Boolean): String? {
        if (!forceRefresh) return currentToken
        forcedRefreshCount += 1
        val token = forcedRefresh()
        if (token != null) currentToken = token
        return token
    }
}
