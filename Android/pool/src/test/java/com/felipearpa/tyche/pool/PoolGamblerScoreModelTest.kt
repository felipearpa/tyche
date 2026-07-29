package com.felipearpa.tyche.pool

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PoolGamblerScoreModelTest {
    @Test
    fun `rank movement is positive up negative down and zero steady`() {
        score(position = 2, beforePosition = 5).rank() shouldBe 3
        score(position = 5, beforePosition = 2).rank() shouldBe -3
        score(position = 2, beforePosition = 2).rank() shouldBe 0
    }

    @Test
    fun `rank movement is unknown when either position is missing`() {
        score(position = null, beforePosition = 2).rank() shouldBe null
        score(position = 2, beforePosition = null).rank() shouldBe null
    }

    private fun score(position: Int?, beforePosition: Int?) = PoolGamblerScoreModel(
        poolId = "pool",
        poolName = "Pool",
        gamblerId = "gambler",
        gamblerUsername = "A very long username that must remain available",
        position = position,
        beforePosition = beforePosition,
        score = null,
        gamblerCount = 10,
    )
}
