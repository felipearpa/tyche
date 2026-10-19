package com.felipearpa.tyche.bet.pending

import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.data.bet.domain.Bet
import com.felipearpa.tyche.data.bet.domain.KickoffGroup
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

/**
 * Serves pending-feed pages by cursor and holds each submitted prediction until the test
 * completes it, so the saving state can be observed before the result arrives. Nothing reaches a
 * backend.
 */
class ScriptedPendingBetRepository(
    private val pages: List<CursorPage<PoolGamblerBet>> = emptyList(),
) : PoolGamblerBetRepository {
    var submissions: List<Bet> = emptyList()
        private set
    private var awaitingReplies: List<CompletableDeferred<Result<PoolGamblerBet>>> = emptyList()

    val submissionCount: Int get() = submissions.size

    /** Submissions that have not been answered yet. */
    val awaitingCount: Int get() = awaitingReplies.size

    override suspend fun bet(bet: Bet): Result<PoolGamblerBet> {
        val reply = CompletableDeferred<Result<PoolGamblerBet>>()
        submissions = submissions + bet
        awaitingReplies = awaitingReplies + reply
        return reply.await()
    }

    /** Answers the oldest held submission with a success that stores its scores. */
    fun completeSaving(submitted: Bet) {
        complete(
            Result.success(
                PendingBetFixtures.bet(
                    submitted.matchId,
                    betScore = TeamScore(submitted.homeTeamBet.value, submitted.awayTeamBet.value),
                ),
            ),
        )
    }

    /** Answers the oldest held submission. */
    fun complete(result: Result<PoolGamblerBet>) {
        val reply = awaitingReplies.first()
        awaitingReplies = awaitingReplies.drop(1)
        reply.complete(result)
    }

    override suspend fun getPendingPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerBet>> {
        if (next == null) return Result.success(pages.firstOrNull() ?: CursorPage(emptyList(), null))
        val index = pages.indexOfFirst { page -> page.next == next } + 1
        return Result.success(pages.getOrNull(index)?.takeIf { index > 0 } ?: CursorPage(emptyList(), null))
    }

    override suspend fun getFinishedPoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
    ): Result<CursorPage<KickoffGroup>> = Result.failure(UnusedInTheseTests())

    override suspend fun getLivePoolGamblerBets(
        poolId: String,
        gamblerId: String,
        next: String?,
        searchText: String?,
    ): Result<CursorPage<PoolGamblerBet>> = Result.failure(UnusedInTheseTests())

    override suspend fun getRankedPoolMatchGamblerBets(
        poolId: String,
        matchId: String,
        next: String?,
    ): Result<CursorPage<PoolGamblerBet>> = Result.failure(UnusedInTheseTests())

    override suspend fun getPoolGamblerBet(
        poolId: String,
        gamblerId: String,
        matchId: String,
    ): Result<PoolGamblerBet> = Result.failure(UnusedInTheseTests())

    override suspend fun getGamblerBetsTimeline(
        poolId: String,
        gamblerId: String,
        next: String?,
    ): Result<CursorPage<KickoffGroup>> = Result.failure(UnusedInTheseTests())

    private class UnusedInTheseTests : Throwable()
}

/** Records the outcomes a row reports. */
class PendingBetOutcomeLog : PendingBetRowOutcomes {
    sealed interface Event {
        data class Saved(val matchId: String) : Event
        data class Failed(val matchId: String) : Event
        data class FailureCleared(val matchId: String) : Event
        data class Locked(val matchId: String) : Event
    }

    var events: List<Event> = emptyList()
        private set

    override fun saved(matchId: String) = record(Event.Saved(matchId))
    override fun failed(matchId: String) = record(Event.Failed(matchId))
    override fun failureCleared(matchId: String) = record(Event.FailureCleared(matchId))
    override fun locked(matchId: String) = record(Event.Locked(matchId))

    private fun record(event: Event) {
        events = events + event
    }
}

/**
 * Every state a row passes through, consecutive repeats collapsed. [scope] must resume collectors
 * eagerly (an unconfined test dispatcher) so each change is sampled as it happens.
 */
class RowStateHistory(private val row: PendingBetRowHolder, scope: CoroutineScope) {
    private var samples: List<PendingBetRowHolder.RowState> = emptyList()

    init {
        scope.launch {
            merge(row.match, row.draft, row.saveState).collect { samples = samples + row.rowState }
        }
    }

    val states: List<PendingBetRowHolder.RowState>
        get() = samples.fold(emptyList()) { states, state -> if (states.lastOrNull() == state) states else states + state }
}

object PendingBetFixtures {
    const val POOL_ID = "pool123"
    const val GAMBLER_ID = "gambler456"

    /** An open, uncomputed match kicking off on Friday, 12 June 2026, 17:00, plus [kickoffDay] days. */
    fun bet(
        matchId: String,
        betScore: TeamScore<Int>? = null,
        isLocked: Boolean = false,
        kickoffDay: Int = 0,
    ) = PoolGamblerBet(
        poolId = POOL_ID,
        gamblerId = GAMBLER_ID,
        gamblerUsername = "El mono",
        matchId = matchId,
        homeTeamId = "co",
        homeTeamName = "Colombia",
        awayTeamId = "br",
        awayTeamName = "Brazil",
        awayTeamScore = null,
        matchScore = null,
        betScore = betScore,
        score = null,
        matchDateTime = LocalDateTime(year = 2026, month = 6, day = 12 + kickoffDay, hour = 17, minute = 0),
        isLocked = isLocked,
        isComputed = false,
    )

    fun match(
        matchId: String,
        betScore: TeamScore<Int>? = null,
        isLocked: Boolean = false,
        kickoffDay: Int = 0,
    ): PoolGamblerBetModel = bet(matchId, betScore, isLocked, kickoffDay).toPoolGamblerBetModel()

    fun page(matches: List<PoolGamblerBetModel>, next: String? = null) =
        CursorPage(
            items = matches.map { match ->
                bet(match.matchId, match.betScore, match.isLocked).copy(matchDateTime = match.matchDateTime)
            },
            next = next,
        )
}

/** A submission's scores. */
fun Bet.scores() = TeamScore(homeTeamBet.value, awayTeamBet.value)
