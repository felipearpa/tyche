package com.felipearpa.bet.infrastructure

import com.felipearpa.appCore.type.BetScore
import com.felipearpa.bet.domain.Bet
import com.felipearpa.bet.domain.BetException
import com.felipearpa.bet.domain.PoolGamblerBetRemoteDataSource
import com.felipearpa.bet.domain.PoolGamblerBetResponse
import com.felipearpa.core.network.HttpStatusCode
import com.felipearpa.core.network.NetworkException
import com.felipearpa.core.network.NetworkExceptionHandler
import com.felipearpa.core.paging.CursorPage
import com.felipearpa.core.type.Ulid
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PoolGamblerBetRepositoryTest {

    private val poolGamblerBetRemoteDataSource = mockk<PoolGamblerBetRemoteDataSource>()

    private val networkExceptionHandler = mockk<NetworkExceptionHandler>()

    private val poolGamblerBetRemoteRepository = PoolGamblerBetRemoteRepository(
        poolGamblerBetRemoteDataSource = poolGamblerBetRemoteDataSource,
        networkExceptionHandler = networkExceptionHandler
    )

    @Before
    fun setUp() {
        clearAllMocks()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `given a http exception with code 'forbidden' when bet is performed then a forbidden failure result is returned`() =
        runTest {
            val httpException =
                NetworkException.HttpException(httpStatusCode = HttpStatusCode.FORBIDDEN)

            coEvery { networkExceptionHandler.handle<CursorPage<PoolGamblerBetResponse>>(block = any()) } returns
                    Result.failure(
                        httpException
                    )

            val result = poolGamblerBetRemoteRepository.bet(
                bet = Bet(
                    poolId = Ulid.randomUlid(),
                    gamblerId = Ulid.randomUlid(),
                    matchId = Ulid.randomUlid(),
                    homeTeamBet = BetScore(0),
                    awayTeamBet = BetScore(0)
                )
            )

            assertTrue(result.isFailure)
            assertEquals(BetException.Forbidden, result.exceptionOrNull())
        }
}