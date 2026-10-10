package com.felipearpa.tyche.bet.match

import com.felipearpa.network.HttpStatus
import com.felipearpa.network.NetworkException
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.data.bet.application.GetPoolGamblerBet
import com.felipearpa.tyche.data.bet.application.GetRankedPoolMatchGamblerBets
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.data.bet.domain.RankedMatchBetsException
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

    private val getPoolGamblerBet = mockk<GetPoolGamblerBet>()
    private val getRankedBets = mockk<GetRankedPoolMatchGamblerBets>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { getRankedBets.execute(any(), any(), any()) } returns
            Result.success(CursorPage(items = emptyList(), next = null))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given the route is the viewer and betting is open when the screen loads then the heading bet is the own bet`() =
        runTest {
            coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } returns Result.success(openMatch(VIEWER, TeamScore(3, 2)))
            val viewModel = viewModel(routedGamblerId = VIEWER)

            viewModel.load()

            viewModel.heading.value.confirmedPresentation shouldBe MatchBetsPresentation.HIDDEN
            viewModel.ownBet.value shouldBe OwnBetState.Bet(TeamScore(3, 2))
            coVerify(exactly = 1) { getPoolGamblerBet.execute(any(), any(), any()) }
        }

    @Test
    fun `given the route is another gambler when betting is open then the own bet is the viewer's not the route's`() =
        runTest {
            coEvery { getPoolGamblerBet.execute(POOL, OTHER, MATCH) } returns Result.success(openMatch(OTHER, TeamScore(1, 0)))
            coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } returns Result.success(openMatch(VIEWER, TeamScore(3, 2)))
            val viewModel = viewModel(routedGamblerId = OTHER)

            viewModel.load()

            viewModel.ownBet.value shouldBe OwnBetState.Bet(TeamScore(3, 2))
        }

    @Test
    fun `given the viewer has no record for the match when the own bet loads then it is a confirmed absence`() =
        runTest {
            coEvery { getPoolGamblerBet.execute(POOL, OTHER, MATCH) } returns Result.success(openMatch(OTHER, null))
            coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } returns
                Result.failure(NetworkException.Http(HttpStatus.NOT_FOUND))
            val viewModel = viewModel(routedGamblerId = OTHER)

            viewModel.load()

            viewModel.ownBet.value shouldBe OwnBetState.NoBet
        }

    @Test
    fun `given the own bet fails when it is retried then only the own bet is requested and it loads`() = runTest {
        coEvery { getPoolGamblerBet.execute(POOL, OTHER, MATCH) } returns Result.success(openMatch(OTHER, null))
        coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } returnsMany listOf(
            Result.failure(NetworkException.RemoteCommunication),
            Result.success(openMatch(VIEWER, TeamScore(2, 2))),
        )
        val viewModel = viewModel(routedGamblerId = OTHER)

        viewModel.load()
        viewModel.ownBet.value shouldBe OwnBetState.Failed

        viewModel.retryOwnBet()

        viewModel.ownBet.value shouldBe OwnBetState.Bet(TeamScore(2, 2))
        coVerify(exactly = 1) { getPoolGamblerBet.execute(POOL, OTHER, MATCH) }
    }

    @Test
    fun `given a confirmed own bet when its refresh fails then the confirmed bet stays`() = runTest {
        coEvery { getPoolGamblerBet.execute(POOL, OTHER, MATCH) } returns Result.success(openMatch(OTHER, null))
        coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } returnsMany listOf(
            Result.success(openMatch(VIEWER, TeamScore(1, 1))),
            Result.failure(NetworkException.RemoteCommunication),
        )
        val viewModel = viewModel(routedGamblerId = OTHER)

        viewModel.load()
        viewModel.refresh()

        viewModel.ownBet.value shouldBe OwnBetState.Bet(TeamScore(1, 1))
    }

    @Test
    fun `given the gate is unknown or hidden when the participant feed is observed then nothing is requested`() =
        runTest {
            val pending = CompletableDeferred<Result<PoolGamblerBet>>()
            coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } coAnswers { pending.await() }
            val viewModel = viewModel(routedGamblerId = VIEWER)

            observeParticipants(viewModel)
            viewModel.load()
            pending.complete(Result.success(openMatch(VIEWER, TeamScore(3, 2))))

            viewModel.heading.value.confirmedPresentation shouldBe MatchBetsPresentation.HIDDEN
            coVerify(exactly = 0) { getRankedBets.execute(any(), any(), any()) }
        }

    @Test
    fun `given betting is closed when the participant feed is observed then the ranked first page is requested`() =
        runTest {
            coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } returns Result.success(closedMatch(VIEWER))
            val viewModel = viewModel(routedGamblerId = VIEWER)

            observeParticipants(viewModel)
            viewModel.load()

            viewModel.heading.value.confirmedPresentation shouldBe MatchBetsPresentation.BETS_CLOSED
            coVerify(exactly = 1) { getRankedBets.execute(POOL, MATCH, null) }
        }

    @Test
    fun `given the server answers that predictions are hidden when the feed loads then the hidden state replaces the rows`() =
        runTest {
            coEvery { getPoolGamblerBet.execute(POOL, OTHER, MATCH) } returns Result.success(closedMatch(OTHER))
            coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } returns Result.success(openMatch(VIEWER, TeamScore(0, 1)))
            coEvery { getRankedBets.execute(any(), any(), any()) } returns
                Result.failure(RankedMatchBetsException.PredictionsHidden())
            val viewModel = viewModel(routedGamblerId = OTHER)

            observeParticipants(viewModel)
            viewModel.load()

            viewModel.heading.value.confirmedPresentation shouldBe MatchBetsPresentation.HIDDEN
            viewModel.ownBet.value shouldBe OwnBetState.Bet(TeamScore(0, 1))
            // The hidden state stops the feed: one ranked request, no retry loop.
            coVerify(exactly = 1) { getRankedBets.execute(any(), any(), any()) }
        }

    @Test
    fun `given revealed bets when a refresh reports the match open again then the hidden state replaces them`() =
        runTest {
            coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } returnsMany listOf(
                Result.success(closedMatch(VIEWER)),
                Result.success(openMatch(VIEWER, TeamScore(2, 0))),
            )
            val viewModel = viewModel(routedGamblerId = VIEWER)

            viewModel.load()
            viewModel.heading.value.confirmedPresentation shouldBe MatchBetsPresentation.BETS_CLOSED
            viewModel.refresh()

            viewModel.heading.value.confirmedPresentation shouldBe MatchBetsPresentation.HIDDEN
            viewModel.ownBet.value shouldBe OwnBetState.Bet(TeamScore(2, 0))
        }

    @Test
    fun `given a confirmed heading when a refresh fails then the heading stays with a failed refresh status`() =
        runTest {
            coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } returnsMany listOf(
                Result.success(computedMatch(VIEWER)),
                Result.failure(NetworkException.RemoteCommunication),
            )
            val viewModel = viewModel(routedGamblerId = VIEWER)

            viewModel.load()
            viewModel.refresh()

            val presentation = viewModel.heading.value.presentation
                .shouldBeInstanceOf<MatchHeadingState.Presentation.Loaded>()
            presentation.presentation shouldBe MatchBetsPresentation.COMPUTED
            presentation.refreshStatus shouldBe MatchHeadingState.RefreshStatus.FAILED
        }

    @Test
    fun `given the first heading request fails when it is retried then the placeholder returns until it loads`() =
        runTest {
            val retry = CompletableDeferred<Result<PoolGamblerBet>>()
            coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } returns
                Result.failure(NetworkException.RemoteCommunication) coAndThen { retry.await() }
            val viewModel = viewModel(routedGamblerId = VIEWER)

            viewModel.load()
            viewModel.heading.value.presentation shouldBe MatchHeadingState.Presentation.Failed

            viewModel.load()
            viewModel.heading.value.presentation shouldBe MatchHeadingState.Presentation.Placeholder

            retry.complete(Result.success(computedMatch(VIEWER)))
            viewModel.heading.value.confirmedPresentation shouldBe MatchBetsPresentation.COMPUTED
        }

    @Test
    fun `given an older heading request finishes after a newer one when both answer then the newer answer stays`() =
        runTest {
            val older = CompletableDeferred<Result<PoolGamblerBet>>()
            coEvery { getPoolGamblerBet.execute(POOL, VIEWER, MATCH) } coAnswers { older.await() } andThen
                Result.success(computedMatch(VIEWER))
            val viewModel = viewModel(routedGamblerId = VIEWER)

            viewModel.load()
            viewModel.refresh()
            older.complete(Result.success(openMatch(VIEWER, null)))

            viewModel.heading.value.confirmedPresentation shouldBe MatchBetsPresentation.COMPUTED
        }

    private fun TestScope.observeParticipants(viewModel: MatchBetListViewModel) {
        // Presents the data so Paging requests the first page, as the list does.
        val presenter = object : PagingDataPresenter<PoolGamblerBetModel>() {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<PoolGamblerBetModel>) = Unit
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.participantBets.collectLatest(presenter::collectFrom)
        }
    }

    private fun viewModel(routedGamblerId: String) = MatchBetListViewModel(
        poolId = POOL,
        routedGamblerId = routedGamblerId,
        matchId = MATCH,
        viewerAccountId = VIEWER,
        getPoolGamblerBet = getPoolGamblerBet,
        getRankedPoolMatchGamblerBets = getRankedBets,
    )

    private fun openMatch(gamblerId: String, betScore: TeamScore<Int>?) = PoolGamblerBet(
        poolId = POOL,
        gamblerId = gamblerId,
        gamblerUsername = gamblerId,
        matchId = MATCH,
        homeTeamId = "co",
        homeTeamName = "Colombia",
        awayTeamId = "br",
        awayTeamName = "Brazil",
        awayTeamScore = null,
        matchScore = null,
        betScore = betScore,
        score = null,
        matchDateTime = LocalDateTime(year = 2026, month = 6, day = 14, hour = 18, minute = 0),
        isLocked = false,
        isComputed = false,
    )

    private fun closedMatch(gamblerId: String) = openMatch(gamblerId, TeamScore(2, 1)).copy(isLocked = true)

    private fun computedMatch(gamblerId: String) =
        closedMatch(gamblerId).copy(isComputed = true, matchScore = TeamScore(1, 0), score = 4)
}

private const val POOL = "pool-1"
private const val MATCH = "match-1"
private const val VIEWER = "viewer"
private const val OTHER = "other-gambler"
