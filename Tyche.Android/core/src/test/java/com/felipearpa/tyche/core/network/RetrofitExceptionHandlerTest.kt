package com.felipearpa.tyche.core.network

import io.mockk.clearAllMocks
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RetrofitExceptionHandlerTest {

    private val retrofitExceptionHandler = RetrofitExceptionHandler()

    @Before
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `given a success function when is handled then a success result is returned`() =
        runTest {
            val expectedResult = "expectedResult"
            val block: suspend () -> String = { expectedResult }

            val result = retrofitExceptionHandler.handle(block)

            assertTrue(result.isSuccess)
            assertEquals(expectedResult, result.getOrNull())
        }

    @Test
    fun `given a HttpException failure function when is handled then a http failure result is returned`() =
        runTest {
            val expectedHttpStatusCode = 500
            val block = `http failure function`(httpStatusCode = expectedHttpStatusCode)

            val result = retrofitExceptionHandler.handle(block)

            `assert failure is http`(
                result = result,
                expectedHttpStatusCode = expectedHttpStatusCode
            )
        }

    private fun `http failure function`(httpStatusCode: Int): suspend () -> String {
        val block: suspend () -> String = {
            throw HttpException(
                Response.error<String>(
                    httpStatusCode,
                    ResponseBody.create(
                        MediaType.get("plain/text"), ""
                    )
                )
            )
        }
        return block
    }

    private fun `assert failure is http`(result: Result<String>, expectedHttpStatusCode: Int) {
        assertTrue(result.isFailure)

        assertTrue(result.exceptionOrNull() is NetworkException.Http)

        assertEquals(
            expectedHttpStatusCode,
            (result.exceptionOrNull() as NetworkException.Http).httpStatusCode.value
        )
    }

    @Test
    fun `given a UnknownHostException failure function when is handled then a remote communication failure result is returned`() =
        runTest {
            val block: suspend () -> String = { throw UnknownHostException() }

            val result = retrofitExceptionHandler.handle(block)

            `assert failure is remote communication`(result = result)
        }

    @Test
    fun `given a SocketTimeoutException failure function when is handled then a remote communication failure result is returned`() =
        runTest {
            val block: suspend () -> String = { throw SocketTimeoutException() }

            val result = retrofitExceptionHandler.handle(block)

            `assert failure is remote communication`(result = result)
        }

    private fun `assert failure is remote communication`(result: Result<String>) {
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException.RemoteCommunication)
    }

    @Test
    fun `given a RuntimeException failure function when is handled then the same exception is returned`() =
        runTest {
            val block: suspend () -> String = { throw RuntimeException() }

            val result = retrofitExceptionHandler.handle(block)

            `assert failure is a runtime exception`(result = result)
        }

    private fun `assert failure is a runtime exception`(result: Result<String>) {
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }
}