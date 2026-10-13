package com.felipearpa.tyche.data.bet.domain

import kotlin.time.Instant

/**
 * One item of the grouped History and Timeline feeds: the matches of a pool that share one
 * scheduled kickoff instant. The server sends whole groups, so the counts and movement describe
 * the complete group whatever page a client has loaded.
 */
data class KickoffGroup(
    /** The group's identity: its kickoff instant. Groups are compared by instant, never by a local date-time. */
    val kickoffDateTime: Instant,
    /** The layout's match count at this kickoff, independent of the rows visible to the caller. */
    val totalMatches: Int,
    /** How many of those matches have a result. */
    val computedMatches: Int,
    /**
     * The gambler's positions before and after the group, from stored snapshots. `null` when the
     * gambler has no computed match in the group or a snapshot is missing.
     */
    val movement: GroupMovement?,
    /** The group's visible matches, ordered by match identifier. */
    val matches: List<PoolGamblerBet>,
)

/** A gambler's position before a kickoff group's first settlement and after its latest one. */
data class GroupMovement(val beforePosition: Int, val position: Int)
