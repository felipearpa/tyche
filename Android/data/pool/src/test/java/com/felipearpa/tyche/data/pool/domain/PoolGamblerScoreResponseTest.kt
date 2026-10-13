package com.felipearpa.tyche.data.pool.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.time.Instant

/** The score row's additive `trendGroup`, decoded the way the app's client decodes responses. */
class PoolGamblerScoreResponseTest {

    @Test
    fun `given a trend group when a score row is decoded then its kickoff instant and counts are kept`() {
        val score = decode(row(trendGroup = """{"kickoffDateTime":"2026-06-12T17:00:00Z","computedMatches":1,"totalMatches":2}"""))

        score.trendGroup shouldBe TrendGroup(
            kickoffDateTime = Instant.parse("2026-06-12T17:00:00Z"),
            computedMatches = 1,
            totalMatches = 2,
        )
        score.position shouldBe 5
        score.beforePosition shouldBe 8
    }

    @Test
    fun `given a null trend group when a score row is decoded then the row has none`() {
        decode(row(trendGroup = "null")).trendGroup.shouldBeNull()
    }

    @Test
    fun `given a response without the trend group field when a score row is decoded then the row has none`() {
        decode(row(trendGroup = null)).trendGroup.shouldBeNull()
    }

    @Test
    fun `given a trend group and no previous position when a score row is decoded then both are kept as sent`() {
        val score = decode(
            row(
                trendGroup = """{"kickoffDateTime":"2026-06-12T17:00:00Z","computedMatches":2,"totalMatches":2}""",
                beforePosition = "null",
            ),
        )

        score.beforePosition.shouldBeNull()
        score.trendGroup?.computedMatches shouldBe 2
    }

    @Test
    fun `given a trend kickoff that is not an instant when a score row is decoded then it fails`() {
        shouldThrow<IllegalArgumentException> {
            decode(row(trendGroup = """{"kickoffDateTime":"12/06/2026","computedMatches":1,"totalMatches":2}"""))
        }
    }

    private fun decode(body: String): PoolGamblerScore =
        json.decodeFromString<PoolGamblerScoreResponse>(body).toPoolGamblerScore()

    private fun row(trendGroup: String?, beforePosition: String = "8"): String {
        val trendGroupField = trendGroup?.let { ""","trendGroup":$it""" }.orEmpty()
        return """
            {"poolId":"pool-1","poolName":"Copa","gamblerId":"gambler-1","gamblerUsername":"El mono",
             "position":5,"beforePosition":$beforePosition,"score":12,"beforeScore":10,"gamblerCount":13$trendGroupField}
        """.trimIndent()
    }
}

/** The app's client configuration (`NetworkProvider`). */
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
