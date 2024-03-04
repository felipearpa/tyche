package com.felipearpa.tyche.core.network

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class NetworkExceptionTest {
    @TestFactory
    fun `given a NetworkException failure result and a transform function when a NetworkException is recover then a transformed failure result is returned`() =
        listOf<Result<String>>(
            Result.failure(NetworkException.RemoteCommunication),
            Result.failure(NetworkException.Http(HttpStatusCode.INTERNAL_SERVER_ERROR))
        ).map { networkFailureResult ->
            dynamicTest("given $networkFailureResult and a transform function when a NetworkException is recover then a transformed failure result is returned") {
                val transformNetworkExceptionFunc = transformNetworkExceptionFunc()
                val actualResult =
                    networkFailureResult.recoverNetworkException(transformNetworkExceptionFunc)
                `verify transformed result was applied to NetworkException`(
                    transformNetworkExceptionFunc = transformNetworkExceptionFunc,
                    networkFailureResult.exceptionOrNull() as NetworkException,
                    actualResult = actualResult
                )
            }
        }

    @Test
    fun `given a non NetworkException failure result and a transform function when a NetworkException is recover then the source result is returned`() {
        val transformNetworkExceptionFunc = transformNetworkExceptionFunc()
        val actualResult =
            nonNetworkFailureResult.recoverNetworkException(transformNetworkExceptionFunc)
        `verify transformed result was not applied to NetworkException`(
            transformNetworkExceptionFunc = transformNetworkExceptionFunc,
            actualResult = actualResult
        )
    }

    @Test
    fun `given a HttpException failure result and a transform function when a HttpException recover then a transformed failure result is returned`() {
        val transformHttpNetworkExceptionFunc = transformHttpNetworkExceptionFunc()
        val transformedResult =
            httpFailureResult.recoverHttpException(transformHttpNetworkExceptionFunc)
        `verify transformed result was applied to Http NetworkException`(
            transformHttpNetworkExceptionFunc = transformHttpNetworkExceptionFunc,
            httpFailureResult.exceptionOrNull() as NetworkException.Http,
            actualResult = transformedResult
        )
    }

    @Test
    fun `given a non HttpException failure result and a transform function when a HttpException recover then the source result is returned`() {
        val transformHttpExceptionFunc = transformHttpNetworkExceptionFunc()
        val nonTransformedResult =
            nonNetworkFailureResult.recoverHttpException(transformHttpExceptionFunc)
        `verify transformed result was not applied to Http NetworkException`(
            transformHttpNetworkExceptionFunc = transformHttpExceptionFunc,
            actualResult = nonTransformedResult
        )
    }

    private fun `verify transformed result was applied to NetworkException`(
        transformNetworkExceptionFunc: (NetworkException) -> Throwable,
        networkException: NetworkException,
        actualResult: Result<String>
    ) {
        actualResult.isFailure.shouldBeTrue()

        val actualTransformedException = actualResult.exceptionOrNull()!!
        actualTransformedException.shouldBeTypeOf<RuntimeException>()

        verify { transformNetworkExceptionFunc(networkException) }
    }

    private fun `verify transformed result was applied to Http NetworkException`(
        transformHttpNetworkExceptionFunc: (NetworkException.Http) -> Throwable,
        httpNetworkException: NetworkException.Http,
        actualResult: Result<String>
    ) {
        actualResult.isFailure.shouldBeTrue()
        actualResult.exceptionOrNull()!! shouldBeEqual transformedException
        verify { transformHttpNetworkExceptionFunc(httpNetworkException) }
    }

    private fun `verify transformed result was not applied to NetworkException`(
        transformNetworkExceptionFunc: (NetworkException) -> Throwable,
        actualResult: Result<String>
    ) {
        actualResult shouldBeEqual nonNetworkFailureResult
        verify(exactly = 0) { transformNetworkExceptionFunc(any()) }
    }

    private fun `verify transformed result was not applied to Http NetworkException`(
        transformHttpNetworkExceptionFunc: (NetworkException.Http) -> Throwable,
        actualResult: Result<String>
    ) {
        actualResult shouldBeEqual nonNetworkFailureResult
        verify(exactly = 0) { transformHttpNetworkExceptionFunc(any()) }
    }

    private fun transformNetworkExceptionFunc(): (NetworkException) -> Throwable {
        val transformNetworkExceptionFunc: (NetworkException) -> Throwable =
            mockk<(NetworkException) -> Throwable>().also { block ->
                every { block(any()) } returns transformedException
            }
        return transformNetworkExceptionFunc
    }

    private fun transformHttpNetworkExceptionFunc(): (NetworkException.Http) -> Throwable {
        val transformHttpNetworkExceptionFunc: (NetworkException.Http) -> Throwable =
            mockk<(NetworkException.Http) -> Throwable>().also { block ->
                every { block(any()) } returns transformedException
            }
        return transformHttpNetworkExceptionFunc
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    companion object {
        val nonNetworkFailureResult: Result<String> = Result.failure(RuntimeException())
        val transformedException = RuntimeException()
        val httpFailureResult: Result<String> =
            Result.failure(NetworkException.Http(HttpStatusCode.INTERNAL_SERVER_ERROR))
    }
}