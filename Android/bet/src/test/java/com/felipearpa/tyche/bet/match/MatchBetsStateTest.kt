package com.felipearpa.tyche.bet.match

import com.felipearpa.tyche.core.type.TeamScore
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MatchBetsStateTest {

    private val open = participant("viewer").copy(isLocked = false, isComputed = false, matchScore = null)

    @Test
    fun `given the server flags when the presentation is chosen then computation wins over the lock`() {
        MatchBetsPresentation.of(open) shouldBe MatchBetsPresentation.HIDDEN
        MatchBetsPresentation.of(open.copy(isLocked = true)) shouldBe MatchBetsPresentation.BETS_CLOSED
        MatchBetsPresentation.of(open.copy(isLocked = true, isComputed = true)) shouldBe MatchBetsPresentation.COMPUTED
        // A computed record whose lock flag is clear still shows awards.
        MatchBetsPresentation.of(open.copy(isComputed = true)) shouldBe MatchBetsPresentation.COMPUTED
    }

    @Test
    fun `given the server hid predictions when the presentation is chosen then it is hidden whatever the flags say`() {
        MatchBetsPresentation.of(open.copy(isLocked = true, isComputed = true), predictionsHiddenByServer = true) shouldBe
            MatchBetsPresentation.HIDDEN
    }

    @Test
    fun `given each state when the header center is chosen then scores are never invented`() {
        val closed = open.copy(isLocked = true)
        MatchHeaderCenter.of(open.copy(matchScore = TeamScore(1, 0)), MatchBetsPresentation.HIDDEN) shouldBe
            MatchHeaderCenter.Versus
        MatchHeaderCenter.of(closed, MatchBetsPresentation.BETS_CLOSED) shouldBe MatchHeaderCenter.Versus
        MatchHeaderCenter.of(closed.copy(matchScore = TeamScore(1, 0)), MatchBetsPresentation.BETS_CLOSED) shouldBe
            MatchHeaderCenter.MatchScore(TeamScore(1, 0))
        MatchHeaderCenter.of(closed.copy(matchScore = TeamScore(0, 0)), MatchBetsPresentation.COMPUTED) shouldBe
            MatchHeaderCenter.FinalScore(TeamScore(0, 0))
        MatchHeaderCenter.of(closed, MatchBetsPresentation.COMPUTED) shouldBe MatchHeaderCenter.FinalScore(null)
    }

    @Test
    fun `given a heading state when it refreshes or fails then the confirmed match stays and the gate is known`() {
        val loaded = MatchHeadingState.initial.loaded(open.copy(isLocked = true))

        MatchHeadingState.initial.presentation shouldBe MatchHeadingState.Presentation.Placeholder
        MatchHeadingState.initial.requesting(MatchHeadingState.Request.LOADING).failed().presentation shouldBe
            MatchHeadingState.Presentation.Failed
        loaded.failed().confirmedPresentation shouldBe MatchBetsPresentation.BETS_CLOSED
        (loaded.failed().presentation as MatchHeadingState.Presentation.Loaded).refreshStatus shouldBe
            MatchHeadingState.RefreshStatus.FAILED
        loaded.hiddenByServer().confirmedPresentation shouldBe MatchBetsPresentation.HIDDEN
        loaded.hiddenByServer().loaded(open.copy(isLocked = true)).confirmedPresentation shouldBe
            MatchBetsPresentation.BETS_CLOSED
    }
}
