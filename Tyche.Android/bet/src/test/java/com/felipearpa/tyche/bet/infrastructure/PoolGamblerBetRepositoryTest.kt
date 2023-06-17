package com.felipearpa.tyche.bet.infrastructure

import com.felipearpa.tyche.bet.domain.Bet
import com.felipearpa.tyche.bet.domain.BetException
import com.felipearpa.tyche.bet.domain.PoolGamblerBetRemoteDataSource
import com.felipearpa.tyche.bet.domain.PoolGamblerBetResponse
import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.type.BetScore
import com.felipearpa.tyche.core.type.Ulid
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
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

    @Test
    fun `given a http exception with code 'forbidden' when bet is performed then a forbidden failure result is returned`() =
        runTest {
            val httpException =
                NetworkException.Http(httpStatusCode = HttpStatusCode.FORBIDDEN)

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