package com.felipearpa.tyche.bet.kickoffgroup

import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.core.type.KickoffGroupProgress
import com.felipearpa.tyche.core.type.RankMovement
import com.felipearpa.tyche.data.bet.domain.KickoffGroup
import kotlin.time.Instant

/**
 * One kickoff group of History or Timeline, ready to present: the server's kickoff instant,
 * counts, and movement, with its match rows in the server's order. Nothing here is recounted or
 * derived from the loaded rows.
 */
data class KickoffGroupModel(
    /** The group's identity. Groups are told apart by instant, never by a formatted or local date-time. */
    val kickoffDateTime: Instant,
    val totalMatches: Int,
    val computedMatches: Int,
    val movement: RankMovement,
    val matches: List<PoolGamblerBetModel>,
)

/** The group's lazy-list key: its kickoff instant, which a feed returns once. */
val KickoffGroupModel.key: String
    get() = "kickoff:$kickoffDateTime"

fun KickoffGroup.toKickoffGroupModel() =
    KickoffGroupModel(
        kickoffDateTime = kickoffDateTime,
        totalMatches = totalMatches,
        computedMatches = computedMatches,
        movement = RankMovement.ofKickoffGroup(
            beforePosition = movement?.beforePosition,
            position = movement?.position,
            progress = KickoffGroupProgress(computedMatches = computedMatches, totalMatches = totalMatches),
            hasComputedMatch = matches.any { it.isComputed },
        ),
        matches = matches.map { it.toPoolGamblerBetModel() },
    )
