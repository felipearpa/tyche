package com.felipearpa.network.retrofit

import com.felipearpa.network.NetworkException
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RetrofitExceptionHandlerTest {
    private val retrofitExceptionHandler = RetrofitExceptionHandler()

    @Test
    fun `given a function when is handled then a success result is returned`() =
        runTest {
            val block: suspend () -> String = { BLOCK_VALUE }
            val result = retrofitExceptionHandler.handle(block)
            `verify if the result is success`(result = result)
        }

    @Test
    fun `given a HttpException failure function when is handled then a http failure result is returned`() =
        runTest {
            val block = `http failure function`()
            val result = retrofitExceptionHandler.handle(block)
            `verify failure is http`(result = result)
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

    @Test
    fun `given a RuntimeException failure function when is handled then the same exception is returned`() =
        runTest {
            val block: suspend () -> String = { throw RuntimeException() }

            val result = retrofitExceptionHandler.handle(block)

            `assert failure is a runtime exception`(result = result)
        }

    private fun `verify if the result is success`(result: Result<String>) {
        result.isSuccess.shouldBeTrue()

        val actualValue = result.getOrNull()
        actualValue.shouldNotBeNull()
        actualValue shouldBe BLOCK_VALUE
    }

    private fun `http failure function`(): suspend () -> String {
        val block: suspend () -> String = {
            throw HttpException(
                Response.error<String>(
                    FAILED_STATUS_CODE,
                    "".toResponseBody("plain/text".toMediaType()),
                ),
            )
        }
        return block
    }

    private fun `verify failure is http`(result: Result<String>) {
        result.isFailure.shouldBeTrue()
        val exception = result.exceptionOrNull().shouldNotBeNull()
        with(exception) {
            shouldBeTypeOf<NetworkException.Http>()
            httpStatus.code shouldBe FAILED_STATUS_CODE
        }
    }

    private fun `assert failure is a runtime exception`(result: Result<String>) {
        result.isFailure.shouldBeTrue()
        result.exceptionOrNull()!!.shouldBeTypeOf<RuntimeException>()
    }

    private fun `assert failure is remote communication`(result: Result<String>) {
        result.isFailure.shouldBeTrue()
        result.exceptionOrNull()!!.shouldBeTypeOf<NetworkException.RemoteCommunication>()
    }
}

private const val BLOCK_VALUE = "result"
private const val FAILED_STATUS_CODE = 500
