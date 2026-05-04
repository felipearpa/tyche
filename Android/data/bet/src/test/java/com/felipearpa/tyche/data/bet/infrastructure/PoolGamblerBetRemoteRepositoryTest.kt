package com.felipearpa.tyche.data.bet.infrastructure

import com.felipearpa.network.HttpStatus
import com.felipearpa.network.NetworkException
import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.type.BetScore
import com.felipearpa.tyche.data.bet.domain.Bet
import com.felipearpa.tyche.data.bet.domain.BetException
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetDataSource
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoolGamblerBetRemoteRepositoryTest {

    private val bet =
        Bet(
            poolId = "01K1PX1TX2NM1HG851S1V0QG6N",
            gamblerId = "01K1PX1TX2NM1HG851S1V0QG6P",
            matchId = "01K1PX1TX2NM1HG851S1V0QG6Q",
            homeTeamBet = BetScore(1),
            awayTeamBet = BetScore(0),
        )

    @Test
    fun `given a 403 from the network when bet is called then result fails with BetException Forbidden`() = runTest {
        val dataSource = mockk<PoolGamblerBetDataSource>()
        val handler =
            object : NetworkExceptionHandler {
                override suspend fun <Value> handle(block: suspend () -> Value): Result<Value> =
                    Result.failure(NetworkException.Http(httpStatus = HttpStatus.FORBIDDEN))
            }

        val repository = PoolGamblerBetRemoteRepository(dataSource, handler)

        val result = repository.bet(bet)

        assertTrue(result.isFailure)
        result.exceptionOrNull().shouldNotBeNull()
        result.exceptionOrNull().shouldBeInstanceOf<BetException.Forbidden>()
    }

    @Test
    fun `given a non-403 http error when bet is called then result fails with NetworkException Http`() = runTest {
        val dataSource = mockk<PoolGamblerBetDataSource>()
        val handler =
            object : NetworkExceptionHandler {
                override suspend fun <Value> handle(block: suspend () -> Value): Result<Value> =
                    Result.failure(NetworkException.Http(httpStatus = HttpStatus.INTERNAL_SERVER_ERROR))
            }

        val repository = PoolGamblerBetRemoteRepository(dataSource, handler)

        val result = repository.bet(bet)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkException.Http)
        assertEquals(
            HttpStatus.INTERNAL_SERVER_ERROR,
            (exception as NetworkException.Http).httpStatus,
        )
    }

    @Test
    fun `given a non-network exception when bet is called then result fails with the original exception`() = runTest {
        val dataSource = mockk<PoolGamblerBetDataSource>()
        val original = IllegalStateException("kaboom")
        val handler =
            object : NetworkExceptionHandler {
                override suspend fun <Value> handle(block: suspend () -> Value): Result<Value> =
                    Result.failure(original)
            }

        val repository = PoolGamblerBetRemoteRepository(dataSource, handler)

        val result = repository.bet(bet)

        assertTrue(result.isFailure)
        assertEquals(original, result.exceptionOrNull())
    }
}
