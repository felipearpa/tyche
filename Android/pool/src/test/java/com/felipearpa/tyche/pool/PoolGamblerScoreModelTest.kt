package com.felipearpa.tyche.pool

import com.felipearpa.tyche.core.type.KickoffGroupProgress
import com.felipearpa.tyche.core.type.RankMovement
import com.felipearpa.tyche.data.pool.domain.PoolGamblerScore
import com.felipearpa.tyche.data.pool.domain.TrendGroup
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class PoolGamblerScoreModelTest {
    private val complete = KickoffGroupProgress(computedMatches = 2, totalMatches = 2)

    @Test
    fun `rank movement is positive up negative down and zero steady`() {
        score(position = 2, beforePosition = 5).movement() shouldBe RankMovement.Moved(3, partialProgress = null)
        score(position = 5, beforePosition = 2).movement() shouldBe RankMovement.Moved(-3, partialProgress = null)
        score(position = 2, beforePosition = 2).movement() shouldBe RankMovement.Moved(0, partialProgress = null)
    }

    @Test
    fun `rank movement is unavailable when the trend group exists and either position is missing`() {
        score(position = null, beforePosition = 2).movement() shouldBe RankMovement.Unavailable
        score(position = 2, beforePosition = null).movement() shouldBe RankMovement.Unavailable
    }

    @Test
    fun `rank movement is pending without a trend group`() {
        score(position = 2, beforePosition = 5, trendGroup = null).movement() shouldBe RankMovement.Pending
    }

    @Test
    fun `a partial trend group keeps the server's counts`() {
        val partial = KickoffGroupProgress(computedMatches = 1, totalMatches = 2)

        score(position = 5, beforePosition = 8, trendGroup = partial).movement() shouldBe
            RankMovement.Moved(places = 3, partialProgress = partial)
    }

    @Test
    fun `the leaderboard and My pools rows take the same counts from the score response`() {
        val response = PoolGamblerScore(
            poolId = "pool",
            poolName = "Pool",
            gamblerId = "gambler",
            gamblerUsername = "El mono",
            position = 5,
            beforePosition = 8,
            score = 12,
            gamblerCount = 10,
            trendGroup = TrendGroup(
                kickoffDateTime = Instant.parse("2026-06-12T17:00:00Z"),
                computedMatches = 2,
                totalMatches = 4,
            ),
        )

        response.toPoolGamblerScoreModel().movement() shouldBe RankMovement.Moved(
            places = 3,
            partialProgress = KickoffGroupProgress(computedMatches = 2, totalMatches = 4),
        )
    }

    private fun score(position: Int?, beforePosition: Int?, trendGroup: KickoffGroupProgress? = complete) =
        PoolGamblerScoreModel(
            poolId = "pool",
            poolName = "Pool",
            gamblerId = "gambler",
            gamblerUsername = "A very long username that must remain available",
            position = position,
            beforePosition = beforePosition,
            score = null,
            gamblerCount = 10,
            trendGroup = trendGroup,
        )
}
