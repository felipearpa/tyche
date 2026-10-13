package com.felipearpa.tyche.data.pool.domain

import kotlin.time.Instant

/**
 * The pool's trend kickoff group: the newest kickoff with a settled match, whose baseline the
 * score rows' `beforePosition` holds. Both counts come from the pool's layout.
 */
data class TrendGroup(
    val kickoffDateTime: Instant,
    val computedMatches: Int,
    val totalMatches: Int,
)
