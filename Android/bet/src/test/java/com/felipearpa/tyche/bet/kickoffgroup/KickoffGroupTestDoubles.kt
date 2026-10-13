package com.felipearpa.tyche.bet.kickoffgroup

import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.data.bet.domain.GroupMovement
import com.felipearpa.tyche.data.bet.domain.KickoffGroup
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import kotlin.time.Instant

/** A grouped-feed page as the data layer returns it. */
internal fun groupPage(vararg groups: KickoffGroup, next: String?) = CursorPage(items = groups.toList(), next = next)

/** A kickoff group at [kickoff] (an ISO-8601 instant) holding [matches]. */
internal fun kickoffGroup(
    kickoff: String,
    vararg matches: PoolGamblerBet,
    totalMatches: Int = matches.size,
    computedMatches: Int = matches.count { it.isComputed },
    movement: GroupMovement? = GroupMovement(beforePosition = 8, position = 6),
) = KickoffGroup(
    kickoffDateTime = Instant.parse(kickoff),
    totalMatches = totalMatches,
    computedMatches = computedMatches,
    movement = movement,
    matches = matches.toList(),
)

internal fun settledBet(matchId: String, score: Int? = 2, gamblerId: String = "gambler") =
    bet(matchId, gamblerId).copy(matchScore = TeamScore(1, 2), score = score, isComputed = true)

internal fun pendingBet(matchId: String, matchScore: TeamScore<Int>? = null, gamblerId: String = "gambler") =
    bet(matchId, gamblerId).copy(matchScore = matchScore, score = null, isComputed = false)

private fun bet(matchId: String, gamblerId: String) = PoolGamblerBet(
    poolId = "pool",
    gamblerId = gamblerId,
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

/** Presents grouped paging data the way a list does, so reading the last group requests the next page. */
internal class RecordingGroupPresenter : PagingDataPresenter<KickoffGroupModel>(mainContext = Dispatchers.Main) {
    var stop: () -> Unit = {}

    /** Keeps reading the last group until no further page arrives. */
    var readToEnd: () -> Unit = {}

    /** The loaded groups' kickoffs, in list order. */
    fun kickoffs() = snapshot().map { group -> group?.kickoffDateTime?.toString() }

    override suspend fun presentPagingDataEvent(event: PagingDataEvent<KickoffGroupModel>) = Unit
}
