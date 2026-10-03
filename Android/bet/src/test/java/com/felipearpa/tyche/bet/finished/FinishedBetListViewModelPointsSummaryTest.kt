package com.felipearpa.tyche.bet.finished

import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.HistoryPointsSummaryState.Presentation
import com.felipearpa.tyche.bet.finished.HistoryPointsSummaryState.RefreshStatus
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.data.bet.application.GetFinishedPoolGamblerBets
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScore
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScore
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * History's earned-points total: requested on entry, on pull to refresh, and on summary retry;
 * independent of the rows; and never replaced by a superseded response.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FinishedBetListViewModelPointsSummaryTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given a positive total when History opens then the summary shows the points`() = runTest {
        val viewModel = viewModel(scores(Result.success(score(120))))

        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(120), RefreshStatus.CURRENT)
    }

    @Test
    fun `given a zero total when History opens then the summary shows zero rather than unavailable`() = runTest {
        val viewModel = viewModel(scores(Result.success(score(0))))

        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(0), RefreshStatus.CURRENT)
    }

    @Test
    fun `given a response without a total when History opens then the summary is unavailable, not zero`() =
        runTest {
            val viewModel = viewModel(scores(Result.success(score(null))))

            viewModel.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Unavailable, RefreshStatus.CURRENT)
        }

    @Test
    fun `given a pending first request then the summary is a placeholder`() = runTest {
        val gate = CompletableDeferred<Result<PoolGamblerScore>>()
        val viewModel = viewModel(scores { gate.await() })

        viewModel.pointsSummary.value.presentation shouldBe Presentation.Placeholder

        gate.complete(Result.success(score(4)))
        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(4), RefreshStatus.CURRENT)
    }

    @Test
    fun `given a failed first request when the summary is retried then the placeholder returns and the total loads`() =
        runTest {
            val gate = CompletableDeferred<Result<PoolGamblerScore>>()
            val getPoolGamblerScore = mockk<GetPoolGamblerScore>()
            coEvery { getPoolGamblerScore.execute(POOL_ID, GAMBLER_ID) } returns
                Result.failure(IllegalStateException("offline")) coAndThen { gate.await() }
            val viewModel = viewModel(getPoolGamblerScore)

            viewModel.pointsSummary.value.presentation shouldBe Presentation.Failed

            viewModel.loadPointsSummary()
            viewModel.pointsSummary.value.presentation shouldBe Presentation.Placeholder

            gate.complete(Result.success(score(7)))
            viewModel.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Earned(7), RefreshStatus.CURRENT)
        }

    @Test
    fun `given a known total when a refresh runs and fails then the total stays with the failure`() = runTest {
        val gate = CompletableDeferred<Result<PoolGamblerScore>>()
        val getPoolGamblerScore = mockk<GetPoolGamblerScore>()
        coEvery { getPoolGamblerScore.execute(POOL_ID, GAMBLER_ID) } returns
            Result.success(score(589)) coAndThen { gate.await() }
        val viewModel = viewModel(getPoolGamblerScore)

        viewModel.refreshPointsSummary()
        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(589), RefreshStatus.REFRESHING)
        viewModel.pointsSummary.value.isPullRefreshing shouldBe true

        gate.complete(Result.failure(IllegalStateException("offline")))
        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(589), RefreshStatus.FAILED)
        viewModel.pointsSummary.value.isPullRefreshing shouldBe false
    }

    @Test
    fun `given a known total when a refresh returns no total then the previous number is replaced`() = runTest {
        val getPoolGamblerScore = mockk<GetPoolGamblerScore>()
        coEvery { getPoolGamblerScore.execute(POOL_ID, GAMBLER_ID) } returnsMany listOf(
            Result.success(score(589)),
            Result.success(score(null)),
        )
        val viewModel = viewModel(getPoolGamblerScore)

        viewModel.refreshPointsSummary()

        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Unavailable, RefreshStatus.CURRENT)
    }

    @Test
    fun `given two refreshes when the older response finishes last then the newer total stays`() = runTest {
        val older = CompletableDeferred<Result<PoolGamblerScore>>()
        val newer = CompletableDeferred<Result<PoolGamblerScore>>()
        val getPoolGamblerScore = mockk<GetPoolGamblerScore>()
        // NonCancellable stands in for a call that answers even after it was superseded.
        coEvery { getPoolGamblerScore.execute(POOL_ID, GAMBLER_ID) } returns
            Result.success(score(10)) coAndThen
            { withContext(NonCancellable) { older.await() } } coAndThen
            { withContext(NonCancellable) { newer.await() } }
        val viewModel = viewModel(getPoolGamblerScore)

        viewModel.refreshPointsSummary()
        viewModel.loadPointsSummary()
        newer.complete(Result.success(score(30)))
        older.complete(Result.success(score(20)))

        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(30), RefreshStatus.CURRENT)
    }

    @Test
    fun `given two refreshes when an older failure finishes last then the newer total is not marked failed`() =
        runTest {
            val older = CompletableDeferred<Result<PoolGamblerScore>>()
            val newer = CompletableDeferred<Result<PoolGamblerScore>>()
            val getPoolGamblerScore = mockk<GetPoolGamblerScore>()
            coEvery { getPoolGamblerScore.execute(POOL_ID, GAMBLER_ID) } returns
                Result.success(score(10)) coAndThen
                { withContext(NonCancellable) { older.await() } } coAndThen
                { withContext(NonCancellable) { newer.await() } }
            val viewModel = viewModel(getPoolGamblerScore)

            viewModel.refreshPointsSummary()
            viewModel.refreshPointsSummary()
            newer.complete(Result.success(score(30)))
            older.complete(Result.failure(IllegalStateException("offline")))

            viewModel.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Earned(30), RefreshStatus.CURRENT)
        }

    @Test
    fun `given a pending request for one pool when another pool's History opens then each shows only its own total`() =
        runTest {
            val firstPoolAnswer = CompletableDeferred<Result<PoolGamblerScore>>()
            val getPoolGamblerScore = mockk<GetPoolGamblerScore>()
            coEvery { getPoolGamblerScore.execute(POOL_ID, GAMBLER_ID) } coAnswers { firstPoolAnswer.await() }
            coEvery { getPoolGamblerScore.execute(OTHER_POOL_ID, GAMBLER_ID) } returns
                Result.success(score(4, poolId = OTHER_POOL_ID))

            val firstPool = viewModel(getPoolGamblerScore, poolId = POOL_ID)
            val otherPool = viewModel(getPoolGamblerScore, poolId = OTHER_POOL_ID)
            firstPoolAnswer.complete(Result.success(score(589)))

            otherPool.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Earned(4), RefreshStatus.CURRENT)
            firstPool.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Earned(589), RefreshStatus.CURRENT)
        }

    @Test
    fun `given loaded rows when further pages load then the total is neither requested again nor changed`() =
        runTest {
            val getFinishedPoolGamblerBets = mockk<GetFinishedPoolGamblerBets>()
            coEvery { getFinishedPoolGamblerBets.execute(POOL_ID, GAMBLER_ID, null, null) } returns
                Result.success(CursorPage(items = bets(prefix = "first", count = 3), next = "page-2"))
            coEvery { getFinishedPoolGamblerBets.execute(POOL_ID, GAMBLER_ID, "page-2", null) } returns
                Result.success(CursorPage(items = bets(prefix = "second", count = 3), next = null))
            val getPoolGamblerScore = scores(Result.success(score(120)))
            val viewModel = viewModel(getPoolGamblerScore, getFinishedPoolGamblerBets)
            val presenter = RecordingPresenter()

            val collection = launch { viewModel.poolGamblerBets.collectLatest(presenter::collectFrom) }
            advanceUntilIdle()
            presenter.size shouldBe 3
            presenter[presenter.size - 1]
            advanceUntilIdle()
            presenter.size shouldBe 6
            collection.cancel()

            coVerify(exactly = 2) { getFinishedPoolGamblerBets.execute(any(), any(), any(), any()) }
            coVerify(exactly = 1) { getPoolGamblerScore.execute(any(), any()) }
            viewModel.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Earned(120), RefreshStatus.CURRENT)
        }

    @Test
    fun `given a failed total when the summary is retried then the rows are not requested again`() = runTest {
        val getFinishedPoolGamblerBets = mockk<GetFinishedPoolGamblerBets>()
        coEvery { getFinishedPoolGamblerBets.execute(POOL_ID, GAMBLER_ID, null, null) } returns
            Result.success(CursorPage(items = bets(prefix = "first", count = 3), next = null))
        val getPoolGamblerScore = mockk<GetPoolGamblerScore>()
        coEvery { getPoolGamblerScore.execute(POOL_ID, GAMBLER_ID) } returnsMany listOf(
            Result.failure(IllegalStateException("offline")),
            Result.success(score(120)),
        )
        val viewModel = viewModel(getPoolGamblerScore, getFinishedPoolGamblerBets)
        val presenter = RecordingPresenter()
        val collection = launch { viewModel.poolGamblerBets.collectLatest(presenter::collectFrom) }
        advanceUntilIdle()

        viewModel.loadPointsSummary()
        advanceUntilIdle()

        presenter.size shouldBe 3
        collection.cancel()
        coVerify(exactly = 1) { getFinishedPoolGamblerBets.execute(any(), any(), any(), any()) }
        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(120), RefreshStatus.CURRENT)
    }

    private fun viewModel(
        getPoolGamblerScore: GetPoolGamblerScore,
        getFinishedPoolGamblerBets: GetFinishedPoolGamblerBets = mockk(),
        poolId: String = POOL_ID,
    ) = FinishedBetListViewModel(
        poolId = poolId,
        gamblerId = GAMBLER_ID,
        getFinishedPoolGamblerBets = getFinishedPoolGamblerBets,
        getPoolGamblerScore = getPoolGamblerScore,
    )

    private fun scores(result: Result<PoolGamblerScore>) = mockk<GetPoolGamblerScore> {
        coEvery { execute(any(), any()) } returns result
    }

    private fun scores(answer: suspend () -> Result<PoolGamblerScore>) = mockk<GetPoolGamblerScore> {
        coEvery { execute(any(), any()) } coAnswers { answer() }
    }

    private fun score(value: Int?, poolId: String = POOL_ID) = PoolGamblerScore(
        poolId = poolId,
        poolName = "Copa",
        gamblerId = GAMBLER_ID,
        gamblerUsername = "felipe",
        position = 8,
        beforePosition = 8,
        score = value,
        gamblerCount = 13,
    )

    private fun bets(prefix: String, count: Int) = (1..count).map { index ->
        PoolGamblerBet(
            poolId = POOL_ID,
            gamblerId = GAMBLER_ID,
            gamblerUsername = "felipe",
            matchId = "$prefix-$index",
            homeTeamId = "es",
            homeTeamName = "España",
            awayTeamId = "ar",
            awayTeamName = "Argentina",
            awayTeamScore = 0,
            matchScore = TeamScore(0, 0),
            betScore = TeamScore(3, 2),
            score = 0,
            matchDateTime = LocalDateTime(year = 2026, month = 7, day = 19, hour = 14, minute = 0),
            isLocked = true,
            isComputed = true,
        )
    }

    /** Presents paging data the way a list does, so reading the last row requests the next page. */
    private class RecordingPresenter : PagingDataPresenter<PoolGamblerBetModel>(
        mainContext = Dispatchers.Main,
    ) {
        override suspend fun presentPagingDataEvent(
            event: PagingDataEvent<PoolGamblerBetModel>,
        ) = Unit
    }
}

private const val POOL_ID = "pool-id"
private const val OTHER_POOL_ID = "other-pool-id"
private const val GAMBLER_ID = "gambler-id"
