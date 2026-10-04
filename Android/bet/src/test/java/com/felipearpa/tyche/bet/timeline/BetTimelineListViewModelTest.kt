package com.felipearpa.tyche.bet.timeline

import androidx.paging.LoadState
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.finished.HistoryPoints
import com.felipearpa.tyche.bet.finished.HistoryPointsSummaryState.Presentation
import com.felipearpa.tyche.bet.finished.HistoryPointsSummaryState.RefreshStatus
import com.felipearpa.tyche.bet.finished.HistoryRowPoints
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.data.bet.application.GetGamblerBetsTimeline
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.data.pool.application.GetPoolGamblerScore
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScore
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
 * Timeline's rows and the selected gambler's authoritative total: requested with the routed pool
 * and gambler, independent of each other, never summed from rows, and never replaced by a
 * superseded response or another gambler's answer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BetTimelineListViewModelTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given a Timeline when it opens then rows and total are requested for the routed pool and gambler`() =
        runTest {
            val timeline = timeline(pages = mapOf(null to page(settled("a", score = 5), next = null)))
            val scores = scores(Result.success(score(120)))
            val viewModel = viewModel(timeline, scores)

            val presenter = collect(viewModel)

            presenter.items() shouldBe listOf("a")
            coVerify(exactly = 1) { timeline.execute(POOL_ID, GAMBLER_ID, null) }
            coVerify(exactly = 1) { scores.execute(POOL_ID, GAMBLER_ID) }
            coVerify(exactly = 0) { scores.execute(neq(POOL_ID), any()) }
            coVerify(exactly = 0) { scores.execute(any(), neq(GAMBLER_ID)) }
        }

    @Test
    fun `given awards on loaded rows when the total loads then it is the server total rather than their sum`() =
        runTest {
            val timeline = timeline(
                pages = mapOf(null to page(settled("a", score = 10), settled("b", score = 5), next = null)),
            )
            val viewModel = viewModel(timeline, scores(Result.success(score(120))))
            collect(viewModel)

            viewModel.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Earned(120), RefreshStatus.CURRENT)
        }

    @Test
    fun `given pending and settled pages when the next page loads then the cursor and order are kept and the total is not requested again`() =
        runTest {
            val timeline = timeline(
                pages = mapOf(
                    null to page(pending("p1"), pending("p2"), next = "page-2"),
                    "page-2" to page(settled("s1", score = 3), settled("s2", score = 0), next = null),
                ),
            )
            val scores = scores(Result.success(score(120)))
            val viewModel = viewModel(timeline, scores)

            val presenter = collect(viewModel, readLastRow = true)

            presenter.items() shouldBe listOf("p1", "p2", "s1", "s2")
            presenter.snapshot().map { bet -> HistoryRowPoints.of(bet!!) } shouldBe listOf(
                HistoryRowPoints.Pending,
                HistoryRowPoints.Pending,
                HistoryRowPoints.Awarded(HistoryPoints.Earned(3)),
                HistoryRowPoints.Awarded(HistoryPoints.Earned(0)),
            )
            coVerify(exactly = 1) { timeline.execute(POOL_ID, GAMBLER_ID, null) }
            coVerify(exactly = 1) { timeline.execute(POOL_ID, GAMBLER_ID, "page-2") }
            coVerify(exactly = 1) { scores.execute(any(), any()) }
        }

    @Test
    fun `given failed rows when the total loads then the total stays usable`() = runTest {
        val timeline = mockk<GetGamblerBetsTimeline> {
            coEvery { execute(any(), any(), any()) } returns Result.failure(IllegalStateException("offline"))
        }
        val viewModel = viewModel(timeline, scores(Result.success(score(120))))

        val presenter = collect(viewModel)

        presenter.loadStateFlow.value?.refresh.shouldBeInstanceOf<LoadState.Error>()
        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(120), RefreshStatus.CURRENT)
    }

    @Test
    fun `given a failed total when the summary is retried then only the total is requested and the rows stay`() =
        runTest {
            val timeline = timeline(pages = mapOf(null to page(settled("a", score = 2), next = null)))
            val scores = mockk<GetPoolGamblerScore>()
            coEvery { scores.execute(POOL_ID, GAMBLER_ID) } returnsMany listOf(
                Result.failure(IllegalStateException("offline")),
                Result.success(score(120)),
            )
            val viewModel = viewModel(timeline, scores)
            val presenter = collect(viewModel)

            viewModel.pointsSummary.value.presentation shouldBe Presentation.Failed
            presenter.items() shouldBe listOf("a")

            viewModel.loadPointsSummary()

            viewModel.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Earned(120), RefreshStatus.CURRENT)
            presenter.items() shouldBe listOf("a")
            coVerify(exactly = 1) { timeline.execute(any(), any(), any()) }
            coVerify(exactly = 2) { scores.execute(POOL_ID, GAMBLER_ID) }
        }

    @Test
    fun `given a known total when a pull refresh fails then the previous total stays marked as failed`() = runTest {
        val gate = CompletableDeferred<Result<PoolGamblerScore>>()
        val scores = mockk<GetPoolGamblerScore>()
        coEvery { scores.execute(POOL_ID, GAMBLER_ID) } returns Result.success(score(676)) coAndThen { gate.await() }
        val viewModel = viewModel(timeline(pages = emptyMap()), scores)

        viewModel.refreshPointsSummary()
        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(676), RefreshStatus.REFRESHING)
        viewModel.pointsSummary.value.isPullRefreshing shouldBe true

        gate.complete(Result.failure(IllegalStateException("offline")))
        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(676), RefreshStatus.FAILED)
        viewModel.pointsSummary.value.isPullRefreshing shouldBe false
    }

    @Test
    fun `given a known total when a refresh succeeds without a total then unavailable replaces the number`() =
        runTest {
            val scores = mockk<GetPoolGamblerScore>()
            coEvery { scores.execute(POOL_ID, GAMBLER_ID) } returnsMany listOf(
                Result.success(score(676)),
                Result.success(score(null)),
            )
            val viewModel = viewModel(timeline(pages = emptyMap()), scores)

            viewModel.refreshPointsSummary()

            viewModel.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Unavailable, RefreshStatus.CURRENT)
        }

    @Test
    fun `given two refreshes when the older response finishes last then the newer total stays`() = runTest {
        val older = CompletableDeferred<Result<PoolGamblerScore>>()
        val newer = CompletableDeferred<Result<PoolGamblerScore>>()
        val scores = mockk<GetPoolGamblerScore>()
        // NonCancellable stands in for a call that answers even after it was superseded.
        coEvery { scores.execute(POOL_ID, GAMBLER_ID) } returns
            Result.success(score(10)) coAndThen
            { withContext(NonCancellable) { older.await() } } coAndThen
            { withContext(NonCancellable) { newer.await() } }
        val viewModel = viewModel(timeline(pages = emptyMap()), scores)

        viewModel.refreshPointsSummary()
        viewModel.refreshPointsSummary()
        newer.complete(Result.success(score(30)))
        older.complete(Result.failure(IllegalStateException("offline")))

        viewModel.pointsSummary.value.presentation shouldBe
            Presentation.Points(HistoryPoints.Earned(30), RefreshStatus.CURRENT)
    }

    @Test
    fun `given a pending request for one gambler when another gambler's Timeline opens then each shows only its own total`() =
        runTest {
            val firstAnswer = CompletableDeferred<Result<PoolGamblerScore>>()
            val scores = mockk<GetPoolGamblerScore>()
            coEvery { scores.execute(POOL_ID, GAMBLER_ID) } coAnswers { firstAnswer.await() }
            coEvery { scores.execute(POOL_ID, OTHER_GAMBLER_ID) } returns
                Result.success(score(4, gamblerId = OTHER_GAMBLER_ID))

            val first = viewModel(timeline(pages = emptyMap()), scores, gamblerId = GAMBLER_ID)
            val other = viewModel(timeline(pages = emptyMap()), scores, gamblerId = OTHER_GAMBLER_ID)
            firstAnswer.complete(Result.success(score(676)))

            other.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Earned(4), RefreshStatus.CURRENT)
            first.pointsSummary.value.presentation shouldBe
                Presentation.Points(HistoryPoints.Earned(676), RefreshStatus.CURRENT)
        }

    @Test
    fun `given a pending row when a refresh returns it computed then it becomes an award`() = runTest {
        val timeline = mockk<GetGamblerBetsTimeline>()
        coEvery { timeline.execute(POOL_ID, GAMBLER_ID, null) } returnsMany listOf(
            Result.success(page(pending("m1", matchScore = TeamScore(1, 0)), next = null)),
            Result.success(page(settled("m1", score = 2), next = null)),
        )
        val viewModel = viewModel(timeline, scores(Result.success(score(120))))
        val presenter = collect(viewModel, keepCollecting = true)
        HistoryRowPoints.of(presenter.snapshot().single()!!) shouldBe HistoryRowPoints.Pending

        presenter.refresh()
        advanceUntilIdle()

        HistoryRowPoints.of(presenter.snapshot().single()!!) shouldBe
            HistoryRowPoints.Awarded(HistoryPoints.Earned(2))
        presenter.stop()
    }

    private fun viewModel(
        getGamblerBetsTimeline: GetGamblerBetsTimeline,
        getPoolGamblerScore: GetPoolGamblerScore,
        gamblerId: String = GAMBLER_ID,
    ) = BetTimelineListViewModel(
        poolId = POOL_ID,
        gamblerId = gamblerId,
        getGamblerBetsTimeline = getGamblerBetsTimeline,
        getPoolGamblerScore = getPoolGamblerScore,
    )

    /** Collects the rows like a list; [readLastRow] reads the last row of each page, which appends. */
    private suspend fun TestScope.collect(
        viewModel: BetTimelineListViewModel,
        readLastRow: Boolean = false,
        keepCollecting: Boolean = false,
    ): RecordingPresenter {
        val presenter = RecordingPresenter()
        val job = launch { viewModel.poolGamblerBets.collectLatest(presenter::collectFrom) }
        advanceUntilIdle()
        if (readLastRow) {
            var size: Int
            do {
                size = presenter.size
                if (size > 0) presenter[size - 1]
                advanceUntilIdle()
            } while (presenter.size > size)
        }
        if (keepCollecting) presenter.stop = { job.cancel() } else job.cancel()
        return presenter
    }

    private fun timeline(pages: Map<String?, CursorPage<PoolGamblerBet>>) = mockk<GetGamblerBetsTimeline> {
        pages.forEach { (next, page) ->
            coEvery { execute(POOL_ID, any(), next) } returns Result.success(page)
        }
    }

    private fun scores(result: Result<PoolGamblerScore>) = mockk<GetPoolGamblerScore> {
        coEvery { execute(any(), any()) } returns result
    }

    private fun score(value: Int?, gamblerId: String = GAMBLER_ID) = PoolGamblerScore(
        poolId = POOL_ID,
        poolName = "Copa",
        gamblerId = gamblerId,
        gamblerUsername = "El mono",
        position = 1,
        beforePosition = 1,
        score = value,
        gamblerCount = 13,
    )

    private fun page(vararg bets: PoolGamblerBet, next: String?) = CursorPage(items = bets.toList(), next = next)

    private fun settled(matchId: String, score: Int?) = bet(matchId).copy(
        matchScore = TeamScore(1, 2),
        score = score,
        isComputed = true,
    )

    private fun pending(matchId: String, matchScore: TeamScore<Int>? = null) = bet(matchId).copy(
        matchScore = matchScore,
        score = null,
        isComputed = false,
    )

    private fun bet(matchId: String) = PoolGamblerBet(
        poolId = POOL_ID,
        gamblerId = GAMBLER_ID,
        gamblerUsername = "El mono",
        matchId = matchId,
        homeTeamId = "gb_eng",
        homeTeamName = "Inglaterra",
        awayTeamId = "ar",
        awayTeamName = "Argentina",
        awayTeamScore = null,
        matchScore = null,
        betScore = TeamScore(2, 1),
        score = null,
        matchDateTime = LocalDateTime(year = 2026, month = 7, day = 19, hour = 14, minute = 0),
        isLocked = true,
        isComputed = false,
    )

    /** Presents paging data the way a list does, so reading the last row requests the next page. */
    private class RecordingPresenter : PagingDataPresenter<PoolGamblerBetModel>(
        mainContext = Dispatchers.Main,
    ) {
        var stop: () -> Unit = {}

        fun items() = snapshot().map { bet -> bet?.matchId }

        override suspend fun presentPagingDataEvent(
            event: PagingDataEvent<PoolGamblerBetModel>,
        ) = Unit
    }
}

private const val POOL_ID = "pool-a"
private const val GAMBLER_ID = "gambler-7"
private const val OTHER_GAMBLER_ID = "gambler-9"
