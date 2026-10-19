package com.felipearpa.tyche.bet.pending

import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.RowState
import com.felipearpa.tyche.core.type.TeamScore
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** What TalkBack hears as a card changes state. */
class PendingBetAnnouncementTest {
    private val placed = SavedBet.Placed(TeamScore(2, 1))

    @Test
    fun `given a save completes then its scores are announced`() {
        PendingBetAnnouncement.of(RowState.SAVING, RowState.SAVED, placed) shouldBe PendingBetAnnouncement.Saved(TeamScore(2, 1))
    }

    @Test
    fun `given a save fails then the failure is announced`() {
        PendingBetAnnouncement.of(RowState.SAVING, RowState.FAILED, placed) shouldBe PendingBetAnnouncement.Failed
    }

    @Test
    fun `given a save is refused because betting closed then the lock is announced`() {
        PendingBetAnnouncement.of(RowState.SAVING, RowState.LOCKED, placed) shouldBe PendingBetAnnouncement.Locked
    }

    @Test
    fun `given other changes then nothing is announced`() {
        PendingBetAnnouncement.of(RowState.SAVED, RowState.SAVING, placed) shouldBe null
        PendingBetAnnouncement.of(RowState.EMPTY, RowState.INCOMPLETE, SavedBet.None) shouldBe null
        PendingBetAnnouncement.of(RowState.FAILED, RowState.SAVED, placed) shouldBe null
        // A refresh that brings the lock is not a refused save.
        PendingBetAnnouncement.of(RowState.SAVED, RowState.LOCKED, placed) shouldBe null
    }
}
