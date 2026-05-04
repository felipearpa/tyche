package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.network.HttpStatus
import com.felipearpa.network.NetworkException
import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.tyche.data.pool.domain.JoinPoolException
import com.felipearpa.tyche.data.pool.domain.JoinPoolInput
import com.felipearpa.tyche.data.pool.domain.PoolDataSource
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoolRemoteRepositoryTest {

    private val joinPoolInput =
        JoinPoolInput(
            poolId = "01K1PX1TX2NM1HG851S1V0QG6N",
            gamblerId = "01K1PX1TX2NM1HG851S1V0QG6P",
        )

    @Test
    fun `given a 409 from the network when joinPool is called then result fails with JoinPoolException AlreadyJoined`() =
        runTest {
            val dataSource = mockk<PoolDataSource>()
            val handler =
                object : NetworkExceptionHandler {
                    override suspend fun <Value> handle(block: suspend () -> Value): Result<Value> =
                        Result.failure(NetworkException.Http(httpStatus = HttpStatus.CONFLICT))
                }

            val repository = PoolRemoteRepository(dataSource, handler)

            val result = repository.joinPool(joinPoolInput)

            assertTrue(result.isFailure)
            result.exceptionOrNull().shouldNotBeNull()
            result.exceptionOrNull().shouldBeInstanceOf<JoinPoolException.AlreadyJoined>()
        }

    @Test
    fun `given a non-409 http error when joinPool is called then result fails with NetworkException Http`() = runTest {
        val dataSource = mockk<PoolDataSource>()
        val handler =
            object : NetworkExceptionHandler {
                override suspend fun <Value> handle(block: suspend () -> Value): Result<Value> =
                    Result.failure(NetworkException.Http(httpStatus = HttpStatus.INTERNAL_SERVER_ERROR))
            }

        val repository = PoolRemoteRepository(dataSource, handler)

        val result = repository.joinPool(joinPoolInput)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkException.Http)
        assertEquals(
            HttpStatus.INTERNAL_SERVER_ERROR,
            (exception as NetworkException.Http).httpStatus,
        )
    }

    @Test
    fun `given a non-network exception when joinPool is called then result fails with the original exception`() =
        runTest {
            val dataSource = mockk<PoolDataSource>()
            val original = IllegalStateException("kaboom")
            val handler =
                object : NetworkExceptionHandler {
                    override suspend fun <Value> handle(block: suspend () -> Value): Result<Value> =
                        Result.failure(original)
                }

            val repository = PoolRemoteRepository(dataSource, handler)

            val result = repository.joinPool(joinPoolInput)

            assertTrue(result.isFailure)
            assertEquals(original, result.exceptionOrNull())
        }
}
