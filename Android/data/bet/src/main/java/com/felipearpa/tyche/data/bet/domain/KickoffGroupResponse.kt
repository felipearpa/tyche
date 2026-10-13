package com.felipearpa.tyche.data.bet.domain

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A grouped History or Timeline item as the server sends it. [kickoffDateTime] is an ISO-8601 UTC
 * instant; `movement` is JSON null when the server has no movement for the group.
 */
@Serializable
internal data class KickoffGroupResponse(
    val kickoffDateTime: String,
    val totalMatches: Int,
    val computedMatches: Int,
    val movement: GroupMovementResponse?,
    val matches: List<PoolGamblerBetResponse>,
)

@Serializable
internal data class GroupMovementResponse(val beforePosition: Int, val position: Int)

/** Throws [IllegalArgumentException] when the kickoff is not an ISO-8601 instant. */
internal fun KickoffGroupResponse.toKickoffGroup() =
    KickoffGroup(
        kickoffDateTime = Instant.parse(kickoffDateTime),
        totalMatches = totalMatches,
        computedMatches = computedMatches,
        movement = movement?.let { GroupMovement(beforePosition = it.beforePosition, position = it.position) },
        matches = matches.map { it.toPoolGamblerBet() },
    )
