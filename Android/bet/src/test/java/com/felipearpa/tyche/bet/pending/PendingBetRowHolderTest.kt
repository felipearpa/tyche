package com.felipearpa.tyche.bet.pending

import com.felipearpa.tyche.bet.pending.PendingBetOutcomeLog.Event
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.Draft
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.RowState
import com.felipearpa.tyche.bet.pending.PendingBetRowHolder.Team
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.data.bet.application.PlaceBet
import com.felipearpa.tyche.data.bet.domain.BetException
import com.felipearpa.ui.state.MutationState
import com.felipearpa.ui.state.activeValue
import com.felipearpa.ui.state.attemptedValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The prediction row's draft, derived state, steppers, and automatic save, driven on virtual time
 * with a repository double so that no prediction reaches a backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PendingBetRowHolderTest {
    private val repository = ScriptedPendingBetRepository()
    private val outcomes = PendingBetOutcomeLog()

    private val empty = PendingBetFixtures.match("m1")
    private val savedTwoOne = PendingBetFixtures.match("m1", betScore = TeamScore(2, 1))

    private fun TestScope.row(match: PoolGamblerBetModel) = PendingBetRowHolder(
        match = match,
        placeBet = PlaceBet(repository),
        scope = backgroundScope,
        outcomes = outcomes,
    )

    private fun TestScope.history(row: PendingBetRowHolder) =
        RowStateHistory(row, CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)))

    // Derived state

    @Test
    fun `given a match without a prediction then the row starts empty`() = runTest {
        val row = row(empty)

        row.rowState shouldBe RowState.EMPTY
        row.draft.value shouldBe Draft(null, null)
        row.saved shouldBe SavedBet.None
    }

    @Test
    fun `given a match with a prediction then the row starts saved with its scores in the draft`() = runTest {
        val row = row(savedTwoOne)

        row.rowState shouldBe RowState.SAVED
        row.draft.value shouldBe Draft(2, 1)
    }

    @Test
    fun `given a locked match when it is edited then it stays locked and submits nothing`() = runTest {
        val row = row(PendingBetFixtures.match("m1", isLocked = true))

        row.setHome(1)
        row.step(Team.AWAY, 1)
        row.setBoth(home = 3, away = 3)
        advanceTimeBy(5.seconds)

        row.rowState shouldBe RowState.LOCKED
        row.draft.value shouldBe Draft(null, null)
        repository.submissionCount shouldBe 0
    }

    @Test
    fun `given one score when time passes then the row is incomplete and never submits`() = runTest {
        val row = row(empty)

        row.setHome(2)
        advanceTimeBy(10.seconds)

        row.rowState shouldBe RowState.INCOMPLETE
        row.draft.value shouldBe Draft(2, null)
        repository.submissionCount shouldBe 0
    }

    @Test
    fun `given a saved prediction when one score is cleared then the row is incomplete and keeps the saved value`() =
        runTest {
            val row = row(savedTwoOne)

            row.setAway(null)
            advanceTimeBy(10.seconds)

            row.rowState shouldBe RowState.INCOMPLETE
            row.saved shouldBe SavedBet.Placed(TeamScore(2, 1))
            repository.submissionCount shouldBe 0
        }

    @Test
    fun `given a changed prediction when it returns to the saved scores then the save is cancelled`() = runTest {
        val row = row(savedTwoOne)

        row.setAway(2)
        row.rowState shouldBe RowState.SAVED
        row.setAway(1)
        advanceTimeBy(5.seconds)

        row.rowState shouldBe RowState.SAVED
        repository.submissionCount shouldBe 0
    }

    @Test
    fun `given a prediction when a negative or out-of-range score is set then it is rejected`() = runTest {
        val row = row(savedTwoOne)

        row.setHome(-1)
        row.setAway(1000)

        row.draft.value shouldBe Draft(2, 1)
    }

    // Steppers

    @Test
    fun `given an empty card when plus is tapped then that team is one and the other zero`() = runTest {
        val home = row(empty)
        val away = row(PendingBetFixtures.match("m2"))

        home.step(Team.HOME, 1)
        away.step(Team.AWAY, 1)

        home.draft.value shouldBe Draft(1, 0)
        away.draft.value shouldBe Draft(0, 1)
        // A complete draft reads as No bet until its debounced save starts.
        away.rowState shouldBe RowState.EMPTY
    }

    @Test
    fun `given an empty card when minus is tapped then both scores are zero`() = runTest {
        val row = row(empty)

        row.step(Team.HOME, -1)

        row.draft.value shouldBe Draft(0, 0)
    }

    @Test
    fun `given a prediction when steppers are tapped then one score changes and stops at zero`() = runTest {
        val row = row(PendingBetFixtures.match("m1", betScore = TeamScore(0, 1)))

        row.step(Team.AWAY, 1)
        row.step(Team.HOME, -1)

        row.draft.value shouldBe Draft(0, 2)
    }

    @Test
    fun `given an incomplete draft when plus is tapped on the missing side then the draft completes and submits`() =
        runTest {
            val row = row(empty)
            row.setHome(2)

            row.step(Team.AWAY, 1)
            advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
            runCurrent()

            row.draft.value shouldBe Draft(2, 1)
            repository.submissions.single().scores() shouldBe TeamScore(2, 1)
        }

    // Automatic save

    @Test
    fun `given both scores typed when the debounce ends then one save is submitted`() = runTest {
        val row = row(empty)
        val history = history(row)

        row.setHome(2)
        row.setAway(1)
        advanceTimeBy(699.milliseconds)
        runCurrent()
        repository.submissionCount shouldBe 0
        row.rowState shouldBe RowState.EMPTY

        advanceTimeBy(1.milliseconds)
        runCurrent()
        row.rowState shouldBe RowState.SAVING
        row.saveState.value shouldBe MutationState.Mutating(
            original = SavedBet.None,
            updated = SavedBet.Placed(TeamScore(2, 1)),
        )
        val submitted = repository.submissions.single()
        submitted.matchId shouldBe "m1"
        submitted.scores() shouldBe TeamScore(2, 1)

        repository.completeSaving(submitted)
        runCurrent()

        row.rowState shouldBe RowState.SAVED
        row.saved shouldBe SavedBet.Placed(TeamScore(2, 1))
        repository.submissionCount shouldBe 1
        outcomes.events shouldBe listOf(Event.Saved("m1"))
        row.saveState.value shouldBe MutationState.Mutated(
            original = SavedBet.None,
            updated = SavedBet.Placed(TeamScore(2, 1)),
        )
        history.states shouldBe listOf(
            RowState.EMPTY,
            RowState.INCOMPLETE,
            RowState.EMPTY,
            RowState.SAVING,
            RowState.SAVED,
        )
    }

    @Test
    fun `given a saved prediction when plus is tapped three times within 300 ms then one submission is made`() =
        runTest {
            val row = row(PendingBetFixtures.match("m1", betScore = TeamScore(0, 0)))
            val history = history(row)

            row.step(Team.HOME, 1)
            advanceTimeBy(150.milliseconds)
            row.step(Team.HOME, 1)
            advanceTimeBy(150.milliseconds)
            row.step(Team.HOME, 1)
            row.rowState shouldBe RowState.SAVED
            history.states shouldBe listOf(RowState.SAVED)

            advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
            runCurrent()
            val submitted = repository.submissions.single()
            repository.completeSaving(submitted)
            runCurrent()

            submitted.scores() shouldBe TeamScore(3, 0)
            repository.submissionCount shouldBe 1
            row.saved shouldBe SavedBet.Placed(TeamScore(3, 0))
            history.states shouldBe listOf(RowState.SAVED, RowState.SAVING, RowState.SAVED)
        }

    @Test
    fun `given a save in flight when the draft changes then the newest scores are resaved once`() = runTest {
        val row = row(empty)
        val history = history(row)
        row.setBoth(home = 2, away = 1)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()
        val first = repository.submissions.single()

        row.setAway(2)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()
        repository.submissionCount shouldBe 1
        row.rowState shouldBe RowState.SAVING

        repository.completeSaving(first)
        runCurrent()
        val second = repository.submissions.last()
        second.scores() shouldBe TeamScore(2, 2)
        // The resave goes from one request straight to the next, from the newly saved scores.
        row.saveState.value shouldBe MutationState.Mutating(
            original = SavedBet.Placed(TeamScore(2, 1)),
            updated = SavedBet.Placed(TeamScore(2, 2)),
        )

        repository.completeSaving(second)
        runCurrent()

        row.rowState shouldBe RowState.SAVED
        repository.submissionCount shouldBe 2
        row.saved shouldBe SavedBet.Placed(TeamScore(2, 2))
        row.draft.value shouldBe Draft(2, 2)
        // The spinner stays up from the first request through the resave.
        history.states shouldBe listOf(RowState.EMPTY, RowState.SAVING, RowState.SAVED)
    }

    @Test
    fun `given a save in flight when the draft returns to the saved scores then they are saved again`() = runTest {
        val row = row(savedTwoOne)
        row.setAway(2)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()
        val first = repository.submissions.single()

        row.setAway(1)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()
        repository.completeSaving(first)
        runCurrent()
        val second = repository.submissions.last()
        repository.completeSaving(second)
        runCurrent()

        row.rowState shouldBe RowState.SAVED
        second.scores() shouldBe TeamScore(2, 1)
        row.saved shouldBe SavedBet.Placed(TeamScore(2, 1))
    }

    // Failure and retry

    @Test
    fun `given a save when it fails then the draft is kept and the failure reported`() = runTest {
        val row = row(empty)

        failSave(row, home = 4, away = 1)

        row.rowState shouldBe RowState.FAILED
        row.draft.value shouldBe Draft(4, 1)
        row.saved shouldBe SavedBet.None
        row.saveState.value.shouldBeInstanceOf<MutationState.Failure<SavedBet>>()
        row.saveState.value.attemptedValue() shouldBe SavedBet.Placed(TeamScore(4, 1))
        outcomes.events shouldBe listOf(Event.Failed("m1"))
    }

    @Test
    fun `given a failed save when retried then the same scores are resubmitted at once`() = runTest {
        val row = row(empty)
        failSave(row, home = 4, away = 1)

        row.retry()
        row.rowState shouldBe RowState.SAVING
        runCurrent()
        val retried = repository.submissions.last()
        repository.completeSaving(retried)
        runCurrent()

        row.rowState shouldBe RowState.SAVED
        retried.scores() shouldBe TeamScore(4, 1)
        repository.submissionCount shouldBe 2
        outcomes.events shouldBe listOf(Event.Failed("m1"), Event.FailureCleared("m1"), Event.Saved("m1"))
    }

    @Test
    fun `given a failed save when a score is edited then the failure clears and the new scores save`() = runTest {
        val row = row(empty)
        failSave(row, home = 4, away = 1)

        row.step(Team.AWAY, 1)
        row.rowState shouldBe RowState.EMPTY
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()

        repository.submissions.last().scores() shouldBe TeamScore(4, 2)
        outcomes.events shouldBe listOf(Event.Failed("m1"), Event.FailureCleared("m1"))
    }

    @Test
    fun `given no failure when retried then nothing is submitted`() = runTest {
        val row = row(savedTwoOne)

        row.retry()
        runCurrent()

        row.rowState shouldBe RowState.SAVED
        repository.submissionCount shouldBe 0
    }

    @Test
    fun `given a forbidden save then the row locks with the last saved prediction`() = runTest {
        val row = row(savedTwoOne)
        row.setHome(3)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()

        repository.complete(Result.failure(BetException.Forbidden()))
        runCurrent()

        row.rowState shouldBe RowState.LOCKED
        row.draft.value shouldBe Draft(2, 1)
        row.saved shouldBe SavedBet.Placed(TeamScore(2, 1))
        row.isEditable shouldBe false
        outcomes.events shouldBe listOf(Event.Locked("m1"))
        row.saveState.value shouldBe MutationState.Idle(SavedBet.Placed(TeamScore(2, 1)))

        row.retry()
        runCurrent()
        repository.submissionCount shouldBe 1
    }

    @Test
    fun `given a forbidden first save then the row locks with empty scores and no failure`() = runTest {
        val row = row(empty)
        row.setBoth(home = 1, away = 1)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()

        repository.complete(Result.failure(BetException.Forbidden()))
        runCurrent()

        row.rowState shouldBe RowState.LOCKED
        row.draft.value shouldBe Draft(null, null)
        row.saveState.value shouldBe MutationState.Idle(SavedBet.None)
    }

    @Test
    fun `given a forbidden save then the lock, saved state, and restored draft land in one change`() = runTest {
        val row = row(savedTwoOne)
        row.setHome(3)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()
        // Collected on the test dispatcher like a frame: it samples only after the holder's
        // synchronous change completes.
        var frames: List<Pair<RowState, Draft>> = emptyList()
        backgroundScope.launch {
            combine(row.match, row.draft, row.saveState) { match, draft, saveState ->
                PendingBetRowHolder.rowStateOf(match, draft, saveState) to draft
            }.collect { frame -> if (frames.lastOrNull() != frame) frames = frames + frame }
        }
        runCurrent()
        frames = emptyList()

        repository.complete(Result.failure(BetException.Forbidden()))
        runCurrent()

        frames shouldBe listOf(RowState.LOCKED to Draft(2, 1))
    }

    // Focus loss

    @Test
    fun `given both scores of a saved prediction cleared when focus leaves then the saved prediction returns`() =
        runTest {
            val row = row(savedTwoOne)
            row.setAway(2)
            row.setHome(null)
            row.setAway(null)

            row.restoreSavedIfCleared()
            advanceTimeBy(5.seconds)

            row.draft.value shouldBe Draft(2, 1)
            row.rowState shouldBe RowState.SAVED
            repository.submissionCount shouldBe 0
        }

    @Test
    fun `given one score of a saved prediction cleared when focus leaves then the row stays incomplete`() = runTest {
        val row = row(savedTwoOne)
        row.setAway(null)

        row.restoreSavedIfCleared()

        row.draft.value shouldBe Draft(2, null)
        row.rowState shouldBe RowState.INCOMPLETE
    }

    @Test
    fun `given an empty card without a prediction when focus leaves then nothing changes`() = runTest {
        val row = row(empty)

        row.restoreSavedIfCleared()

        row.draft.value shouldBe Draft(null, null)
        row.rowState shouldBe RowState.EMPTY
    }

    // Feed updates

    @Test
    fun `given an incomplete draft when the feed rebinds then the draft is kept`() = runTest {
        val row = row(empty)
        row.setHome(2)

        row.bind(empty)

        row.draft.value shouldBe Draft(2, null)
        row.rowState shouldBe RowState.INCOMPLETE
    }

    @Test
    fun `given no draft when the feed brings a new prediction then the row adopts it`() = runTest {
        val row = row(empty)

        row.bind(savedTwoOne)

        row.saved shouldBe SavedBet.Placed(TeamScore(2, 1))
        row.draft.value shouldBe Draft(2, 1)
        row.rowState shouldBe RowState.SAVED
    }

    @Test
    fun `given a pending debounce when the feed brings another prediction then the draft is kept`() = runTest {
        val row = row(empty)
        row.setBoth(home = 3, away = 0)

        row.bind(savedTwoOne)

        row.draft.value shouldBe Draft(3, 0)
        row.saved shouldBe SavedBet.None
    }

    @Test
    fun `given a draft when the feed locks the match then the draft is dropped`() = runTest {
        val row = row(savedTwoOne)
        row.setHome(5)

        row.bind(savedTwoOne.copy(isLocked = true))
        advanceTimeBy(5.seconds)

        row.rowState shouldBe RowState.LOCKED
        row.draft.value shouldBe Draft(2, 1)
        repository.submissionCount shouldBe 0
    }

    @Test
    fun `given a locked row when a stale unlocked page arrives then the row stays locked`() = runTest {
        val row = row(savedTwoOne)
        row.bind(savedTwoOne.copy(isLocked = true))

        row.bind(savedTwoOne)

        row.rowState shouldBe RowState.LOCKED
        row.isEditable shouldBe false
    }

    @Test
    fun `given a save in flight when the feed locks the match then the late result does not unlock it`() = runTest {
        val row = row(savedTwoOne)
        row.setHome(3)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()

        row.bind(savedTwoOne.copy(isLocked = true))
        repository.completeSaving(repository.submissions.single())
        runCurrent()

        row.rowState shouldBe RowState.LOCKED
        row.saved shouldBe SavedBet.Placed(TeamScore(2, 1))
    }

    private fun TestScope.failSave(row: PendingBetRowHolder, home: Int, away: Int) {
        row.setBoth(home = home, away = away)
        advanceTimeBy(PendingBetRowHolder.SAVE_DEBOUNCE)
        runCurrent()
        repository.complete(Result.failure(IOException("offline")))
        runCurrent()
        row.rowState shouldBe RowState.FAILED
    }
}
