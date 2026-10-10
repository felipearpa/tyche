package com.felipearpa.tyche.bet.match

import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import com.felipearpa.network.NetworkException
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.data.bet.domain.RankedMatchBetsException
import com.felipearpa.tyche.ui.network.NetworkLocalizedException
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test

class RankedMatchBetsFeedTest {

    @Test
    fun `given pages from the server when they load then the server order is kept and the cursor is passed on`() =
        runTest {
            val requests = mutableListOf<String?>()
            val feed = feed { next ->
                requests += next
                when (next) {
                    null -> page("leader-with-zero-award", "second", next = "c2")
                    else -> page("third", next = null)
                }
            }
            val source = feed.pagingSource()

            val first = source.load(refresh()).shouldBeInstanceOf<LoadResult.Page<String, PoolGamblerBetModel>>()
            val second = source.load(append("c2")).shouldBeInstanceOf<LoadResult.Page<String, PoolGamblerBetModel>>()

            first.data.map { it.gamblerId } shouldContainExactly listOf("leader-with-zero-award", "second")
            first.nextKey shouldBe "c2"
            second.data.map { it.gamblerId } shouldContainExactly listOf("third")
            second.nextKey shouldBe null
            requests shouldContainExactly listOf(null, "c2")
        }

    @Test
    fun `given a participant moved across a page boundary when the next page loads then it is not shown twice`() =
        runTest {
            val feed = feed { next ->
                when (next) {
                    null -> page("a", "b", next = "c2")
                    else -> page("b", "c", next = null)
                }
            }
            val source = feed.pagingSource()

            source.load(refresh())
            val second = source.load(append("c2")) as LoadResult.Page

            second.data.map { it.gamblerId } shouldContainExactly listOf("c")
        }

    @Test
    fun `given a page emptied by deduplication when it loads then the following page is fetched at once`() =
        runTest {
            val requests = mutableListOf<String?>()
            val feed = feed { next ->
                requests += next
                when (next) {
                    null -> page("a", "b", next = "c2")
                    "c2" -> page("a", "b", next = "c3")
                    else -> page("c", next = null)
                }
            }
            val source = feed.pagingSource()

            source.load(refresh())
            val appended = source.load(append("c2")) as LoadResult.Page

            appended.data.map { it.gamblerId } shouldContainExactly listOf("c")
            requests shouldContainExactly listOf(null, "c2", "c3")
        }

    @Test
    fun `given refills keep finding no new participants when the limit is reached then the request stops with its cursor`() =
        runTest {
            var requests = 0
            val feed = feed { next ->
                requests++
                if (next == null) page("a", next = "c1") else page("a", next = "c${requests}")
            }
            val source = feed.pagingSource()

            source.load(refresh())
            val appended = source.load(append("c1")) as LoadResult.Page

            appended.data shouldBe emptyList()
            appended.nextKey shouldBe "c5"
            requests shouldBe 5
        }

    @Test
    fun `given a new generation when its first page loads then participants of the old generation are shown again`() =
        runTest {
            val feed = feed { page("a", next = null) }

            feed.pagingSource().load(refresh())
            val fresh = feed.pagingSource().load(refresh()) as LoadResult.Page

            fresh.data.map { it.gamblerId } shouldContainExactly listOf("a")
        }

    @Test
    fun `given a rejected continuation when it happens twice then the feed restarts once and then offers a restart`() =
        runTest {
            val feed = feed { Result.failure(RankedMatchBetsException.CursorRejected()) }

            feed.pagingSource().load(append("stale")).shouldBeInstanceOf<LoadResult.Invalid<String, PoolGamblerBetModel>>()
            val second = feed.pagingSource().load(append("stale"))

            second.shouldBeInstanceOf<LoadResult.Error<String, PoolGamblerBetModel>>()
                .throwable.shouldBeInstanceOf<MatchBetsLocalizedException.ListChanged>()
        }

    @Test
    fun `given the restart was used when the viewer restarts the list then one automatic restart is available again`() =
        runTest {
            val feed = feed { Result.failure(RankedMatchBetsException.CursorRejected()) }
            feed.pagingSource().load(append("stale"))

            feed.resetAutomaticRestart()

            feed.pagingSource().load(append("stale")).shouldBeInstanceOf<LoadResult.Invalid<String, PoolGamblerBetModel>>()
        }

    @Test
    fun `given the restart was used when a later append succeeds then one automatic restart is available again`() =
        runTest {
            var rejectNext = true
            val feed = feed { next ->
                if (next != null && rejectNext) Result.failure(RankedMatchBetsException.CursorRejected())
                else page("a", next = "c2")
            }
            feed.pagingSource().load(append("stale"))
            rejectNext = false
            feed.pagingSource().load(append("c1"))
            rejectNext = true

            feed.pagingSource().load(append("stale")).shouldBeInstanceOf<LoadResult.Invalid<String, PoolGamblerBetModel>>()
        }

    @Test
    fun `given predictions are hidden when a page loads then the owner is told and no rows are returned`() = runTest {
        var hiddenReports = 0
        val feed = RankedMatchBetsFeed(
            query = { Result.failure(RankedMatchBetsException.PredictionsHidden()) },
            onPredictionsHidden = { hiddenReports++ },
        )

        feed.pagingSource().load(refresh())
            .shouldBeInstanceOf<LoadResult.Error<String, PoolGamblerBetModel>>()
            .throwable.shouldBeInstanceOf<RankedMatchBetsException.PredictionsHidden>()
        hiddenReports shouldBe 1
    }

    @Test
    fun `given the ranking is unavailable when a page loads then a recoverable ranking failure is shown`() = runTest {
        val feed = feed { Result.failure(RankedMatchBetsException.RankingUnavailable()) }

        feed.pagingSource().load(refresh())
            .shouldBeInstanceOf<LoadResult.Error<String, PoolGamblerBetModel>>()
            .throwable.shouldBeInstanceOf<MatchBetsLocalizedException.RankingUnavailable>()
    }

    @Test
    fun `given the network fails when a page loads then the failure is localized`() = runTest {
        val feed = feed { Result.failure(NetworkException.RemoteCommunication) }

        feed.pagingSource().load(append("c2"))
            .shouldBeInstanceOf<LoadResult.Error<String, PoolGamblerBetModel>>()
            .throwable shouldBe NetworkLocalizedException.RemoteCommunication
    }

    private fun feed(query: suspend (String?) -> Result<CursorPage<PoolGamblerBetModel>>) =
        RankedMatchBetsFeed(query = query, onPredictionsHidden = {})

    private fun refresh(): LoadParams<String> = LoadParams.Refresh(key = null, loadSize = 50, placeholdersEnabled = false)

    private fun append(key: String): LoadParams<String> = LoadParams.Append(key = key, loadSize = 50, placeholdersEnabled = false)

    private fun page(vararg gamblerIds: String, next: String?): Result<CursorPage<PoolGamblerBetModel>> =
        Result.success(CursorPage(items = gamblerIds.map(::participant), next = next))
}

internal fun participant(gamblerId: String) = PoolGamblerBetModel(
    poolId = "pool-1",
    gamblerId = gamblerId,
    gamblerUsername = gamblerId,
    matchId = "match-1",
    homeTeamId = "es",
    homeTeamName = "España",
    awayTeamId = "ar",
    awayTeamName = "Argentina",
    matchScore = TeamScore(0, 0),
    betScore = null,
    score = 0,
    matchDateTime = LocalDateTime(year = 2026, month = 7, day = 19, hour = 14, minute = 0),
    isLocked = true,
    isComputed = true,
)
