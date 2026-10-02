package com.felipearpa.tyche.bet.match

import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.data.bet.application.GetPoolGamblerBet
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import com.felipearpa.ui.state.LoadState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchBetListViewModelTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given a loaded match when it reloads then the loaded match stays until the new answer arrives`() =
        runTest {
            val getPoolGamblerBet = mockk<GetPoolGamblerBet>()
            val gate = CompletableDeferred<Result<PoolGamblerBet>>()
            coEvery { getPoolGamblerBet.execute(any(), any(), any()) } returnsMany listOf(
                Result.success(firstAnswer),
            ) coAndThen { gate.await() }
            val viewModel = matchBetListViewModel(getPoolGamblerBet)

            viewModel.loadPoolGamblerBet()
            val firstState = viewModel.poolGamblerBetState.value
            firstState.loadedMatchScore() shouldBe null

            viewModel.loadPoolGamblerBet()
            viewModel.poolGamblerBetState.value shouldBeSameInstanceAs firstState

            gate.complete(Result.success(secondAnswer))
            viewModel.poolGamblerBetState.value.loadedMatchScore() shouldBe TeamScore(1, 0)
        }

    @Test
    fun `given a failed load when it is retried then the loading presentation returns`() =
        runTest {
            val getPoolGamblerBet = mockk<GetPoolGamblerBet>()
            val gate = CompletableDeferred<Result<PoolGamblerBet>>()
            coEvery { getPoolGamblerBet.execute(any(), any(), any()) } returnsMany listOf(
                Result.failure(IllegalStateException("offline")),
            ) coAndThen { gate.await() }
            val viewModel = matchBetListViewModel(getPoolGamblerBet)

            viewModel.loadPoolGamblerBet()
            (viewModel.poolGamblerBetState.value is LoadState.Failure) shouldBe true

            viewModel.loadPoolGamblerBet()
            viewModel.poolGamblerBetState.value shouldBe LoadState.Loading

            gate.complete(Result.success(firstAnswer))
            (viewModel.poolGamblerBetState.value is LoadState.Loaded) shouldBe true
        }

    /** The loaded match's score; fails when the state is not loaded. */
    private fun LoadState<PoolGamblerBetModel>.loadedMatchScore(): TeamScore<Int>? =
        (this as LoadState.Loaded).value.matchScore

    private fun matchBetListViewModel(getPoolGamblerBet: GetPoolGamblerBet) =
        MatchBetListViewModel(
            poolId = "pool-1",
            gamblerId = "gambler-1",
            matchId = "match-1",
            getPoolGamblerBet = getPoolGamblerBet,
            getPoolMatchGamblerBets = mockk(),
        )

    private val firstAnswer = PoolGamblerBet(
        poolId = "pool-1",
        gamblerId = "gambler-1",
        gamblerUsername = "ElGoleador",
        matchId = "match-1",
        homeTeamId = "co",
        homeTeamName = "Colombia",
        awayTeamId = "br",
        awayTeamName = "Brazil",
        awayTeamScore = null,
        matchScore = null,
        betScore = TeamScore(2, 1),
        score = null,
        matchDateTime = LocalDateTime(year = 2026, month = 6, day = 14, hour = 18, minute = 0),
        isLocked = true,
        isComputed = false,
    )

    private val secondAnswer = firstAnswer.copy(
        matchScore = TeamScore(1, 0),
        isComputed = true,
    )
}
