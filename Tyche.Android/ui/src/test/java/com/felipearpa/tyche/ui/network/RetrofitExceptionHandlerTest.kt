package com.felipearpa.tyche.ui.network

import com.felipearpa.tyche.core.network.RetrofitExceptionHandler
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
    fun `given a function that returns a value when is handled by the handler then a success result is returned`() =
        runTest {
            val expectedResult = "expectedResult"
            val block: suspend () -> String = { expectedResult }

            val result = retrofitExceptionHandler.handle(block)

            assertTrue(result.isSuccess)
            assertEquals(expectedResult, result.getOrNull())
        }

    @Test
    fun `given a function that raises an HttpException when is handled by the handler then a http failure result is returned`() =
        runTest {
            val expectedHttpCode = 500

            val block: suspend () -> String = {
                throw HttpException(
                    Response.error<String>(
                        expectedHttpCode,
                        ResponseBody.create(
                            MediaType.get("plain/text"), ""
                        )
                    )
                )
            }

            val result = retrofitExceptionHandler.handle(block)

            assertTrue(result.isFailure)

            assertTrue(result.exceptionOrNull() is com.felipearpa.tyche.core.network.NetworkException.Http)

            assertEquals(
                expectedHttpCode,
                (result.exceptionOrNull() as com.felipearpa.tyche.core.network.NetworkException.Http).httpStatusCode.value
            )
        }

    @Test
    fun `given a function that raises an UnknownHostException when is handled by the handler then a remote communication failure result is returned`() =
        runTest {
            val expectedHttpCode = 500

            val block: suspend () -> String = { throw UnknownHostException() }

            val result = retrofitExceptionHandler.handle(block)

            assertTrue(result.isFailure)

            assertTrue(result.exceptionOrNull() is com.felipearpa.tyche.core.network.NetworkException.RemoteCommunication)
        }

    @Test
    fun `given a function that raises an SocketTimeoutException when is handled by the handler then a remote communication failure result is returned`() =
        runTest {
            val expectedHttpCode = 500

            val block: suspend () -> String = { throw SocketTimeoutException() }

            val result = retrofitExceptionHandler.handle(block)

            assertTrue(result.isFailure)

            assertTrue(result.exceptionOrNull() is com.felipearpa.tyche.core.network.NetworkException.RemoteCommunication)
        }
}