package com.felipearpa.tyche.bet.kickoffgroup

import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.felipearpa.tyche.bet.finished.HistoryPoints
import com.felipearpa.tyche.bet.finished.HistoryRowPoints
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.paging.map
import com.felipearpa.tyche.core.type.KickoffGroupProgress
import com.felipearpa.tyche.core.type.RankMovement
import com.felipearpa.tyche.data.bet.domain.GroupMovement
import com.felipearpa.tyche.data.bet.domain.KickoffGroup
import com.felipearpa.tyche.data.bet.domain.KickoffGroupFeedException
import com.felipearpa.tyche.ui.exception.UnknownLocalizedException
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The paging rules History and Timeline share: whole groups in server order, one entry per kickoff,
 * one automatic first-page restart after a rejected continuation, and Paging's own retry for any
 * other failed append.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KickoffGroupFeedTest {

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given two pages when the next page loads then its groups follow in server order with the server's counts`() =
        runTest {
            val server = ScriptedServer(
                null to listOf(
                    Result.success(
                        groupPage(
                            kickoffGroup(SUNDAY, settledBet("a"), settledBet("b")),
                            kickoffGroup(SATURDAY, settledBet("c"), totalMatches = 4, computedMatches = 2),
                            next = "page-2",
                        ),
                    ),
                ),
                "page-2" to listOf(Result.success(groupPage(kickoffGroup(FRIDAY, settledBet("d")), next = null))),
            )
            val presenter = present(server, readToEnd = true)

            presenter.kickoffs() shouldContainExactly listOf(SUNDAY, SATURDAY, FRIDAY)
            val saturday = presenter.snapshot()[1]!!
            saturday.totalMatches shouldBe 4
            saturday.computedMatches shouldBe 2
            saturday.movement shouldBe RankMovement.Moved(2, KickoffGroupProgress(computedMatches = 2, totalMatches = 4))
            saturday.matches.map { it.matchId } shouldContainExactly listOf("c")
            server.requests shouldContainExactly listOf(null, "page-2")
            presenter.stop()
        }

    @Test
    fun `given two kickoffs with the same local label when they load then each keeps its own key`() = runTest {
        // New York repeats 01:30 when daylight saving time ends on 2026-11-01.
        val server = ScriptedServer(
            null to listOf(
                Result.success(
                    groupPage(
                        kickoffGroup("2026-11-01T06:30:00Z", settledBet("late")),
                        kickoffGroup("2026-11-01T05:30:00Z", settledBet("early")),
                        next = null,
                    ),
                ),
            ),
        )
        val presenter = present(server)

        presenter.snapshot().map { it!!.key } shouldContainExactly
            listOf("kickoff:2026-11-01T06:30:00Z", "kickoff:2026-11-01T05:30:00Z")
        presenter.stop()
    }

    @Test
    fun `given a group that settles while paging when it reappears in the finished phase then it is shown once`() =
        runTest {
            val server = ScriptedServer(
                null to listOf(
                    Result.success(
                        groupPage(
                            kickoffGroup(SUNDAY, settledBet("a"), pendingBet("b"), computedMatches = 1),
                            next = "finished:1",
                        ),
                    ),
                ),
                "finished:1" to listOf(
                    Result.success(
                        groupPage(
                            kickoffGroup(SUNDAY, settledBet("a"), settledBet("b")),
                            kickoffGroup(SATURDAY, settledBet("c")),
                            next = null,
                        ),
                    ),
                ),
            )
            val presenter = present(server, readToEnd = true)

            presenter.kickoffs() shouldContainExactly listOf(SUNDAY, SATURDAY)
            presenter.snapshot().map { it!!.key }.toSet().size shouldBe 2
            presenter.stop()
        }

    @Test
    fun `given a page whose groups were all shown already when it has a continuation then the next page still loads`() =
        runTest {
            val server = ScriptedServer(
                null to listOf(Result.success(groupPage(kickoffGroup(SUNDAY, settledBet("a")), next = "page-2"))),
                // Every group of this page was already shown, so it adds nothing to the list.
                "page-2" to listOf(Result.success(groupPage(kickoffGroup(SUNDAY, settledBet("a")), next = "page-3"))),
                "page-3" to listOf(Result.success(groupPage(kickoffGroup(SATURDAY, settledBet("b")), next = null))),
            )
            val presenter = present(server, readToEnd = true)

            server.requests shouldContainExactly listOf(null, "page-2", "page-3")
            presenter.kickoffs() shouldContainExactly listOf(SUNDAY, SATURDAY)
            presenter.loadStateFlow.value?.append shouldBe LoadState.NotLoading(endOfPaginationReached = true)
            presenter.stop()
        }

    @Test
    fun `given a partial unsettled group when a refresh finds every match computed then the same group is finished`() =
        runTest {
            val server = ScriptedServer(
                null to listOf(
                    Result.success(
                        groupPage(
                            kickoffGroup(
                                SUNDAY,
                                settledBet("a"),
                                pendingBet("b"),
                                computedMatches = 1,
                                movement = GroupMovement(beforePosition = 8, position = 6),
                            ),
                            next = null,
                        ),
                    ),
                    Result.success(
                        groupPage(
                            kickoffGroup(
                                SUNDAY,
                                settledBet("a"),
                                settledBet("b", score = 3),
                                movement = GroupMovement(beforePosition = 8, position = 5),
                            ),
                            next = null,
                        ),
                    ),
                ),
            )
            val presenter = present(server)
            presenter.snapshot().single()!!.movement shouldBe
                RankMovement.Moved(2, KickoffGroupProgress(computedMatches = 1, totalMatches = 2))

            presenter.refresh()
            advanceUntilIdle()

            val group = presenter.snapshot().single()!!
            group.movement shouldBe RankMovement.Moved(3, partialProgress = null)
            group.matches.map(HistoryRowPoints::of) shouldContainExactly listOf(
                HistoryRowPoints.Awarded(HistoryPoints.Earned(2)),
                HistoryRowPoints.Awarded(HistoryPoints.Earned(3)),
            )
            presenter.stop()
        }

    @Test
    fun `given a rejected continuation when the list appends then it restarts from the first page once`() = runTest {
        val server = ScriptedServer(
            null to listOf(
                Result.success(groupPage(kickoffGroup(SUNDAY, settledBet("a")), next = "stale")),
                Result.success(groupPage(kickoffGroup(SUNDAY, settledBet("a")), next = "fresh")),
            ),
            "stale" to listOf(Result.failure(KickoffGroupFeedException.CursorRejected())),
            "fresh" to listOf(Result.success(groupPage(kickoffGroup(SATURDAY, settledBet("b")), next = null))),
        )
        val presenter = present(server, readToEnd = true)
        // The list reads its visible groups again once the restarted first page arrives.
        presenter.readToEnd()

        server.requests shouldContainExactly listOf(null, "stale", null, "fresh")
        presenter.kickoffs() shouldContainExactly listOf(SUNDAY, SATURDAY)
        presenter.loadStateFlow.value?.append shouldBe LoadState.NotLoading(endOfPaginationReached = true)
        presenter.stop()
    }

    @Test
    fun `given the restarted list is rejected again when it appends then the list offers a restart and stops requesting`() =
        runTest {
            val server = ScriptedServer(
                null to listOf(Result.success(groupPage(kickoffGroup(SUNDAY, settledBet("a")), next = "stale")))
                    .repeatForever(),
                "stale" to listOf(Result.failure<CursorPage<KickoffGroup>>(KickoffGroupFeedException.CursorRejected()))
                    .repeatForever(),
            )
            val presenter = present(server, readToEnd = true)
            // The list reads its visible groups again after the restart, and keeps reading them.
            repeat(3) { presenter.readToEnd() }

            server.requests shouldContainExactly listOf(null, "stale", null, "stale")
            presenter.kickoffs() shouldContainExactly listOf(SUNDAY)
            (presenter.loadStateFlow.value?.append as LoadState.Error).error
                .shouldBeInstanceOf<KickoffGroupFeedLocalizedException.ListChanged>()
            presenter.stop()
        }

    @Test
    fun `given the viewer restarts after a second rejection then one more automatic restart is available`() = runTest {
        val server = ScriptedServer(
            null to listOf(Result.success(groupPage(kickoffGroup(SUNDAY, settledBet("a")), next = "stale")))
                .repeatForever(),
            "stale" to listOf(Result.failure<CursorPage<KickoffGroup>>(KickoffGroupFeedException.CursorRejected()))
                .repeatForever(),
        )
        val presenter = present(server, readToEnd = true)
        presenter.readToEnd()
        server.requests.clear()

        server.feed.resetAutomaticRestart()
        presenter.refresh()
        advanceUntilIdle()
        repeat(3) { presenter.readToEnd() }

        server.requests shouldContainExactly listOf(null, "stale", null, "stale")
        presenter.stop()
    }

    @Test
    fun `given one loaded group and a failed append when the viewer retries then the same continuation is requested`() =
        runTest {
            val server = ScriptedServer(
                null to listOf(Result.success(groupPage(kickoffGroup(SUNDAY, settledBet("a")), next = "page-2"))),
                "page-2" to listOf(
                    Result.failure(IllegalStateException("offline")),
                    Result.success(groupPage(kickoffGroup(SATURDAY, settledBet("b")), next = null)),
                ),
            )
            val presenter = present(server, readToEnd = true)
            (presenter.loadStateFlow.value?.append as LoadState.Error).error
                .shouldBeInstanceOf<UnknownLocalizedException>()

            presenter.retry()
            advanceUntilIdle()

            server.requests shouldContainExactly listOf(null, "page-2", "page-2")
            presenter.kickoffs() shouldContainExactly listOf(SUNDAY, SATURDAY)
            presenter.stop()
        }

    @Test
    fun `given a group without movement when it has no computed match then it is pending, otherwise unavailable`() =
        runTest {
            val server = ScriptedServer(
                null to listOf(
                    Result.success(
                        groupPage(
                            kickoffGroup(SUNDAY, pendingBet("a"), computedMatches = 0, movement = null),
                            kickoffGroup(SATURDAY, settledBet("b"), movement = null),
                            next = null,
                        ),
                    ),
                ),
            )
            val presenter = present(server)

            presenter.snapshot().map { it!!.movement } shouldContainExactly
                listOf(RankMovement.Pending, RankMovement.Unavailable)
            presenter.stop()
        }

    /** Presents the feed like a list; [readToEnd] keeps reading the last group, which appends. */
    private suspend fun TestScope.present(server: ScriptedServer, readToEnd: Boolean = false): RecordingGroupPresenter {
        val presenter = RecordingGroupPresenter()
        val pager = Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 1, enablePlaceholders = false),
            pagingSourceFactory = server.feed::pagingSource,
        )
        val job = launch { pager.flow.collectLatest(presenter::collectFrom) }
        presenter.stop = { job.cancel() }
        presenter.readToEnd = {
            var size: Int
            do {
                size = presenter.size
                if (size > 0) presenter[size - 1]
                advanceUntilIdle()
            } while (presenter.size > size)
        }
        advanceUntilIdle()
        if (readToEnd) presenter.readToEnd()
        return presenter
    }

    /**
     * Answers each cursor with its scripted results in order, records every request, and serves
     * them through a [KickoffGroupFeed].
     */
    private class ScriptedServer(vararg script: Pair<String?, List<Result<CursorPage<KickoffGroup>>>>) {
        private val remaining = script.associate { (cursor, results) -> cursor to results.iterator() }
        val requests = mutableListOf<String?>()
        val feed = KickoffGroupFeed(
            query = { next ->
                requests += next
                remaining.getValue(next).next().map { page -> page.map { it.toKickoffGroupModel() } }
            },
        )
    }
}

private fun <T> List<T>.repeatForever(): List<T> = List(100) { this[it % size] }

private const val SUNDAY = "2026-07-19T19:00:00Z"
private const val SATURDAY = "2026-07-18T16:00:00Z"
private const val FRIDAY = "2026-07-17T20:00:00Z"
