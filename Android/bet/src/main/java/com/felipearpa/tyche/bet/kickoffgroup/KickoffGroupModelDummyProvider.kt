package com.felipearpa.tyche.bet.kickoffgroup

import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.historyBetPlaceholderModel
import com.felipearpa.tyche.bet.historyBetPreviewModels
import com.felipearpa.tyche.bet.timelineBetPreviewModels
import com.felipearpa.tyche.core.type.KickoffGroupProgress
import com.felipearpa.tyche.core.type.RankMovement
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Stable filler for a loading group: a header with a numeric movement and two finished rows, the
 * usual shape of a kickoff. Its kickoff is fixed so the masked date and time keep the same length
 * on every redraw. Never shown as real content.
 */
fun kickoffGroupPlaceholderModel() =
    KickoffGroupModel(
        kickoffDateTime = Instant.parse("2026-01-01T17:00:00Z"),
        totalMatches = 2,
        computedMatches = 2,
        movement = RankMovement.Moved(places = 1, partialProgress = null),
        matches = listOf(
            historyBetPlaceholderModel().copy(matchId = "placeholder-1"),
            historyBetPlaceholderModel().copy(matchId = "placeholder-2"),
        ),
    )

/**
 * History preview groups, newest first: two matches that moved up, one that moved down, a partial
 * four-match group, a steady three-match group with long names and missing values, and a group
 * from an earlier year whose movement is unavailable. Preview data only.
 */
fun historyGroupPreviewModels(now: Instant = Clock.System.now()): List<KickoffGroupModel> {
    val rows = historyBetPreviewModels()
    return listOf(
        previewGroup(now - 1.days, total = 2, computed = 2, movement = moved(3), matches = listOf(rows[1], rows[0])),
        previewGroup(
            now - 2.days,
            total = 1,
            computed = 1,
            movement = moved(-1),
            matches = listOf(rows[0].copy(matchId = "history-single")),
        ),
        previewGroup(
            now - 5.days,
            total = 4,
            computed = 2,
            movement = moved(2, KickoffGroupProgress(computedMatches = 2, totalMatches = 4)),
            matches = listOf(rows[0].copy(matchId = "history-partial-1"), rows[1].copy(matchId = "history-partial-2")),
        ),
        previewGroup(
            now - 6.days,
            total = 3,
            computed = 3,
            movement = moved(0),
            matches = listOf(
                rows[2],
                rows[1].copy(matchId = "history-steady-2"),
                rows[0].copy(matchId = "history-steady-3"),
            ),
        ),
        previewGroup(
            now - 400.days,
            total = 1,
            computed = 1,
            movement = RankMovement.Unavailable,
            matches = listOf(rows[3]),
        ),
    )
}

/**
 * Timeline preview groups: an unsettled group with no result yet (pending movement), an unsettled
 * group with one of two matches computed, then the settled History groups. Preview data only.
 */
fun timelineGroupPreviewModels(now: Instant = Clock.System.now()): List<KickoffGroupModel> {
    val pending = timelineBetPreviewModels()
    return listOf(
        previewGroup(
            now - 1.hours,
            total = 2,
            computed = 0,
            movement = RankMovement.Pending,
            matches = pending.take(2),
        ),
        previewGroup(
            now - 3.hours,
            total = 2,
            computed = 1,
            movement = moved(2, KickoffGroupProgress(computedMatches = 1, totalMatches = 2)),
            matches = listOf(
                historyBetPreviewModels()[0].copy(matchId = "timeline-partial-1"),
                pending[0].copy(matchId = "timeline-partial-2"),
            ),
        ),
    ) + historyGroupPreviewModels(now)
}

/** Groups of one to four matches, for checking the rail and dividers. Preview data only. */
fun groupSizePreviewModels(now: Instant = Clock.System.now()): List<KickoffGroupModel> {
    val rows = historyBetPreviewModels()
    return (1..4).map { size ->
        previewGroup(
            now - size.days,
            total = size,
            computed = size,
            movement = moved(size - 2),
            matches = (0 until size).map { index -> rows[index % 2].copy(matchId = "size-$size-$index") },
        )
    }
}

private fun moved(places: Int, partialProgress: KickoffGroupProgress? = null) =
    RankMovement.Moved(places = places, partialProgress = partialProgress)

private fun previewGroup(
    kickoff: Instant,
    total: Int,
    computed: Int,
    movement: RankMovement,
    matches: List<PoolGamblerBetModel>,
) = KickoffGroupModel(
    kickoffDateTime = kickoff.roundedToMinute(),
    totalMatches = total,
    computedMatches = computed,
    movement = movement,
    matches = matches,
)

private fun Instant.roundedToMinute(): Instant {
    val minute = 1.minutes.inWholeMilliseconds
    return Instant.fromEpochMilliseconds(toEpochMilliseconds() / minute * minute)
}
