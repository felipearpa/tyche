package com.felipearpa.tyche.bet.finished

import com.felipearpa.tyche.bet.historyBetPreviewModels
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HistoryRowPointsTest {
    private val settled = historyBetPreviewModels().first()

    @Test
    fun `a computed entry shows its award, including a confirmed zero`() {
        HistoryRowPoints.of(settled.copy(score = 2)) shouldBe HistoryRowPoints.Awarded(HistoryPoints.Earned(2))
        HistoryRowPoints.of(settled.copy(score = 0)) shouldBe HistoryRowPoints.Awarded(HistoryPoints.Earned(0))
    }

    @Test
    fun `a computed entry without an award is unavailable rather than zero`() {
        HistoryRowPoints.of(settled.copy(score = null)) shouldBe HistoryRowPoints.Awarded(HistoryPoints.Unavailable)
    }

    @Test
    fun `an entry that is not computed is pending even when it carries a score`() {
        HistoryRowPoints.of(settled.copy(isComputed = false, score = 2)) shouldBe HistoryRowPoints.Pending
        HistoryRowPoints.of(settled.copy(isComputed = false, score = null)) shouldBe HistoryRowPoints.Pending
    }
}
