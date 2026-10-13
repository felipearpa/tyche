package com.felipearpa.tyche.core.type

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RankMovementTest {
    private val complete = KickoffGroupProgress(computedMatches = 2, totalMatches = 2)
    private val partial = KickoffGroupProgress(computedMatches = 1, totalMatches = 2)

    @Test
    fun `given no trend group when a score row's movement is derived then it is pending whatever the positions`() {
        RankMovement.ofTrendGroup(progress = null, beforePosition = 8, position = 5) shouldBe RankMovement.Pending
        RankMovement.ofTrendGroup(progress = null, beforePosition = null, position = null) shouldBe
            RankMovement.Pending
    }

    @Test
    fun `given a trend group and a missing position when a score row's movement is derived then it is unavailable`() {
        RankMovement.ofTrendGroup(progress = complete, beforePosition = null, position = 5) shouldBe
            RankMovement.Unavailable
        RankMovement.ofTrendGroup(progress = complete, beforePosition = 8, position = null) shouldBe
            RankMovement.Unavailable
    }

    @Test
    fun `given a complete trend group when a score row's movement is derived then it is the baseline minus the position`() {
        RankMovement.ofTrendGroup(progress = complete, beforePosition = 8, position = 5) shouldBe
            RankMovement.Moved(places = 3, partialProgress = null)
        RankMovement.ofTrendGroup(progress = complete, beforePosition = 4, position = 6) shouldBe
            RankMovement.Moved(places = -2, partialProgress = null)
        RankMovement.ofTrendGroup(progress = complete, beforePosition = 3, position = 3) shouldBe
            RankMovement.Moved(places = 0, partialProgress = null)
    }

    @Test
    fun `given a partial trend group when a score row's movement is derived then it carries the server's counts`() {
        RankMovement.ofTrendGroup(progress = partial, beforePosition = 8, position = 5) shouldBe
            RankMovement.Moved(places = 3, partialProgress = partial)
    }

    @Test
    fun `given a feed group without movement when it has no computed match then it is pending`() {
        RankMovement.ofKickoffGroup(
            beforePosition = null,
            position = null,
            progress = KickoffGroupProgress(computedMatches = 0, totalMatches = 2),
            hasComputedMatch = false,
        ) shouldBe RankMovement.Pending
    }

    @Test
    fun `given a feed group without movement when it has a computed match then it is unavailable, not zero`() {
        RankMovement.ofKickoffGroup(
            beforePosition = null,
            position = null,
            progress = complete,
            hasComputedMatch = true,
        ) shouldBe RankMovement.Unavailable
    }

    @Test
    fun `given a feed group with snapshots when it is partial then the movement keeps the computed count`() {
        RankMovement.ofKickoffGroup(beforePosition = 8, position = 6, progress = partial, hasComputedMatch = true) shouldBe
            RankMovement.Moved(places = 2, partialProgress = partial)
        RankMovement.ofKickoffGroup(beforePosition = 8, position = 8, progress = complete, hasComputedMatch = true) shouldBe
            RankMovement.Moved(places = 0, partialProgress = null)
    }
}
