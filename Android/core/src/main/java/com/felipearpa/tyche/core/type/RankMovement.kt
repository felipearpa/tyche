package com.felipearpa.tyche.core.type

/**
 * How many of a kickoff group's matches have a result. Both counts come from the pool's layout on
 * the server, never from the rows a client has loaded.
 */
data class KickoffGroupProgress(val computedMatches: Int, val totalMatches: Int) {
    /** True while some of the group's matches still lack a result. */
    val isPartial: Boolean
        get() = computedMatches < totalMatches
}

/**
 * A gambler's rank movement across one kickoff group, as every movement surface shows it: the
 * leaderboard, My pools, History, and Timeline.
 */
sealed interface RankMovement {
    /**
     * No settled match yet: the pool has no trend group, or the feed group has no computed match
     * for this gambler. Never shown as a number or as steady.
     */
    data object Pending : RankMovement

    /** The group has a settled match but a position snapshot is missing. Never shown as zero. */
    data object Unavailable : RankMovement

    /**
     * `beforePosition - position`: positive is a gain, negative a loss, zero steady.
     * [partialProgress] is set while the group still has matches without a result.
     */
    data class Moved(val places: Int, val partialProgress: KickoffGroupProgress?) : RankMovement

    companion object {
        /**
         * The leaderboard and My pools movement: the pool's trend group with the row's current and
         * baseline positions. No trend group is pending; a trend group with either position
         * missing is unavailable.
         */
        fun ofTrendGroup(progress: KickoffGroupProgress?, beforePosition: Int?, position: Int?): RankMovement {
            if (progress == null) return Pending
            if (beforePosition == null || position == null) return Unavailable
            return moved(beforePosition = beforePosition, position = position, progress = progress)
        }

        /**
         * A History or Timeline group's movement from the server's snapshots. A missing movement
         * is pending when the gambler has no computed match in the group, and unavailable
         * otherwise.
         */
        fun ofKickoffGroup(
            beforePosition: Int?,
            position: Int?,
            progress: KickoffGroupProgress,
            hasComputedMatch: Boolean,
        ): RankMovement {
            if (beforePosition == null || position == null) return if (hasComputedMatch) Unavailable else Pending
            return moved(beforePosition = beforePosition, position = position, progress = progress)
        }

        private fun moved(beforePosition: Int, position: Int, progress: KickoffGroupProgress) =
            Moved(places = beforePosition - position, partialProgress = progress.takeIf { it.isPartial })
    }
}
