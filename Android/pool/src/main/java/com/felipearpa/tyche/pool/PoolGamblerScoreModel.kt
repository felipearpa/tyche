package com.felipearpa.tyche.pool

import com.felipearpa.tyche.core.type.KickoffGroupProgress
import com.felipearpa.tyche.core.type.RankMovement

data class PoolGamblerScoreModel(
    val poolId: String,
    val poolName: String,
    val gamblerId: String,
    val gamblerUsername: String,
    val position: Int?,
    /** The position before the trend group's first settlement. */
    val beforePosition: Int?,
    val score: Int?,
    val gamblerCount: Int?,
    /** The pool's trend kickoff group's counts; `null` while the pool has no settled kickoff group. */
    val trendGroup: KickoffGroupProgress?,
)

/**
 * The trend group's movement, as the leaderboard and My pools show it: pending without a trend
 * group, unavailable when a position is missing, otherwise `beforePosition - position` with the
 * group's counts while it is partial.
 */
fun PoolGamblerScoreModel.movement(): RankMovement =
    RankMovement.ofTrendGroup(progress = trendGroup, beforePosition = beforePosition, position = position)
