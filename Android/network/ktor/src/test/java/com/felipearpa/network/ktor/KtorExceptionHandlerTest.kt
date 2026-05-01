package com.felipearpa.network.ktor

import com.felipearpa.network.HttpStatus
import com.felipearpa.network.NetworkException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class KtorExceptionHandlerTest {

    @Test
    fun `given a 403 response when handle is called then NetworkException Http with FORBIDDEN is returned`() = runTest {
        val client =
            HttpClient(MockEngine { respondError(HttpStatusCode.Forbidden) }) {
                expectSuccess = true
            }

        val handler = KtorExceptionHandler()

        val result =
            handler.handle<HttpResponse> {
                client.get("https://example.com/anything")
            }

        result.isFailure shouldBe true
        val exception = result.exceptionOrNull()
        exception.shouldBeInstanceOf<NetworkException.Http>()
        exception.httpStatus shouldBe HttpStatus.FORBIDDEN
    }

    @Test
    fun `given a 500 response when handle is called then NetworkException Http with INTERNAL_SERVER_ERROR is returned`() = runTest {
        val client =
            HttpClient(MockEngine { respondError(HttpStatusCode.InternalServerError) }) {
                expectSuccess = true
            }

        val handler = KtorExceptionHandler()

        val result =
            handler.handle<HttpResponse> {
                client.get("https://example.com/anything")
            }

        result.isFailure shouldBe true
        val exception = result.exceptionOrNull()
        exception.shouldBeInstanceOf<NetworkException.Http>()
        exception.httpStatus shouldBe HttpStatus.INTERNAL_SERVER_ERROR
    }
}
