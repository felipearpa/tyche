package com.felipearpa.tyche.session

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AuthenticatedRequestLoggingTest {

    @Test
    fun `given an authenticated request that is retried after a 401 when it is logged then neither token appears in the logs`() =
        runTest {
            val logger = RecordingLogger()
            val engine = MockEngine { request ->
                if (request.headers[HttpHeaders.Authorization] == "Bearer $FRESH_TOKEN") {
                    respond(content = "", status = HttpStatusCode.OK)
                } else {
                    respond(content = "", status = HttpStatusCode.Unauthorized)
                }
            }
            val client = HttpClient(engine) {
                install(Logging) {
                    authenticatedRequestLogging(logger = logger)
                }
                install(Auth) {
                    authTokenBearer(authTokenRetriever = RefreshingAuthTokenRetriever())
                }
            }

            val response = client.get(URL)

            response.status shouldBe HttpStatusCode.OK
            logger.output shouldContain HttpHeaders.Authorization
            logger.output shouldNotContain STALE_TOKEN
            logger.output shouldNotContain FRESH_TOKEN
        }
}

private const val URL = "https://api.example.test/pools"
private const val STALE_TOKEN = "stale-token-value"
private const val FRESH_TOKEN = "fresh-token-value"

private class RecordingLogger : Logger {
    var output = ""
        private set

    override fun log(message: String) {
        output += message + "\n"
    }
}

private class RefreshingAuthTokenRetriever : AuthTokenRetriever {
    private var currentToken = STALE_TOKEN

    override suspend fun authToken(forceRefresh: Boolean): String {
        if (forceRefresh) currentToken = FRESH_TOKEN
        return currentToken
    }
}
