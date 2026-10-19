package com.felipearpa.tyche.bet.pending

import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.core.type.BetScore
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.data.bet.application.PlaceBet
import com.felipearpa.tyche.data.bet.domain.Bet
import com.felipearpa.tyche.data.bet.domain.BetException
import com.felipearpa.tyche.data.bet.domain.PoolGamblerBet
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.ui.state.MutationState
import com.felipearpa.ui.state.activeValue
import com.felipearpa.ui.state.isFailure
import com.felipearpa.ui.state.isMutating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Receives the outcomes of a [PendingBetRowHolder]'s saves, so the row does not depend on the
 * screen that keeps it.
 */
interface PendingBetRowOutcomes {
    /** A prediction was saved; the row's [PendingBetRowHolder.saved] holds it. */
    fun saved(matchId: String)

    /** A save failed and the row now shows the failure with the attempted scores kept. */
    fun failed(matchId: String)

    /** The row left a failure, by an edit or a retry. */
    fun failureCleared(matchId: String)

    /** The server rejected a save because predictions are locked; the row is now read-only. */
    fun locked(matchId: String)
}

/**
 * One pending match's prediction: the gambler's [draft], the last saved prediction, and the
 * automatic save between them. Kept by [PendingBetListViewModel], keyed by match, so the draft and
 * an in-flight save survive paging updates and filtering. All work runs on [scope], the list's
 * `viewModelScope`, and every method must be called on the main thread.
 *
 * [saveState] is a [MutationState] over the [SavedBet]: `Idle` holds the saved prediction
 * (`Idle(None)` before the first save), `Mutating` an in-flight request from the saved scores to the attempted ones, `Mutated` the
 * scores the server confirmed, and `Failure` the attempted scores it did not accept. Its active
 * value is always the saved prediction. The lock lives only on [match].
 *
 * A complete draft that differs from the saved prediction is submitted [SAVE_DEBOUNCE] after the
 * last change. Changes during the debounce restart it; a debounce that ends while a save is in
 * flight queues one resave of the newest draft, sent as soon as that save returns so the row stays
 * saving throughout. An incomplete draft is never submitted.
 */
class PendingBetRowHolder(
    match: PoolGamblerBetModel,
    private val placeBet: PlaceBet,
    private val scope: CoroutineScope,
    private val outcomes: PendingBetRowOutcomes,
) {
    /** The scores in the card's boxes; either may be empty. */
    data class Draft(val home: Int?, val away: Int?) {
        val isIncomplete: Boolean get() = (home == null) != (away == null)

        /** Both scores, or null while either is empty. */
        val complete: TeamScore<Int>?
            get() = if (home != null && away != null) TeamScore(home, away) else null

        companion object {
            fun of(saved: SavedBet) = when (saved) {
                SavedBet.None -> Draft(home = null, away = null)
                is SavedBet.Placed -> Draft(home = saved.score.homeTeamValue, away = saved.score.awayTeamValue)
            }
        }
    }

    enum class Team { HOME, AWAY }

    enum class RowState {
        /** Predictions are closed; the row is read-only. */
        LOCKED,

        /**
         * No prediction is saved and the draft is not incomplete: both scores are empty, or a
         * complete draft is waiting for its debounced save.
         */
        EMPTY,

        /** Exactly one score is entered. */
        INCOMPLETE,

        /** A save request is in flight. */
        SAVING,

        /**
         * A prediction is saved. A changed complete draft waiting for its debounced save, and a
         * draft with both scores cleared before focus leaves the card, keep this state.
         */
        SAVED,

        /** The last save failed; the draft keeps the attempted scores. */
        FAILED,
    }

    private val _match = MutableStateFlow(match)
    val match: StateFlow<PoolGamblerBetModel> = _match.asStateFlow()

    private val _draft = MutableStateFlow(Draft.of(SavedBet.of(match.betScore)))
    val draft: StateFlow<Draft> = _draft.asStateFlow()

    private val _saveState =
        MutableStateFlow<MutationState<SavedBet>>(MutationState.Idle(SavedBet.of(match.betScore)))
    val saveState: StateFlow<MutationState<SavedBet>> = _saveState.asStateFlow()

    private var debounceJob: Job? = null
    private var pendingResave = false

    val matchId: String get() = _match.value.matchId

    val rowState: RowState get() = rowStateOf(_match.value, _draft.value, _saveState.value)

    val isEditable: Boolean get() = !_match.value.isLocked

    /** The last prediction the server accepted. */
    val saved: SavedBet get() = _saveState.value.activeValue()

    /**
     * Takes the feed's latest copy of the match. Server values replace the prediction only while
     * the row has no draft, debounce, save, or failure of its own; a lock applies either way and
     * sticks, so a stale page cannot unlock the row.
     */
    fun bind(model: PoolGamblerBetModel) {
        if (model.isLocked) {
            adoptLocked(model)
            return
        }
        _match.value = if (_match.value.isLocked) model.copy(isLocked = true) else model
        val feedBet = SavedBet.of(model.betScore)
        if (hasLocalWork || saved == feedBet) return
        _saveState.value = MutationState.Idle(feedBet)
        _draft.value = Draft.of(feedBet)
    }

    fun setHome(value: Int?) = setDraft(_draft.value.copy(home = value))

    fun setAway(value: Int?) = setDraft(_draft.value.copy(away = value))

    fun setBoth(home: Int, away: Int) = setDraft(Draft(home = home, away = away))

    /**
     * Changes [team]'s score by [delta], never below zero. With neither score entered, `+` sets
     * the team to 1 and the other to 0, and `−` sets both to 0.
     */
    fun step(team: Team, delta: Int) {
        if (delta == 0) return
        val current = _draft.value
        if (current.home == null && current.away == null) {
            setDraft(firstStep(team, delta))
            return
        }
        when (team) {
            Team.HOME -> setHome(maxOf(0, (current.home ?: 0) + delta))
            Team.AWAY -> setAway(maxOf(0, (current.away ?: 0) + delta))
        }
    }

    /**
     * Called when focus leaves the card. A draft with both scores cleared returns to the saved
     * prediction without a submission; a draft with one score cleared stays incomplete.
     */
    fun restoreSavedIfCleared() {
        val current = _draft.value
        if (!isEditable || current.home != null || current.away != null) return
        when (val saved = saved) {
            SavedBet.None -> return
            is SavedBet.Placed -> _draft.value = Draft.of(saved)
        }
    }

    /** Resubmits the failed scores at once. */
    fun retry() {
        if (!isEditable || !_saveState.value.isFailure()) return
        val score = _draft.value.complete ?: return
        submit(score)
    }

    /** The prediction of the request in flight, or null when no request is in flight. */
    private val inFlight: SavedBet?
        get() = (_saveState.value as? MutationState.Mutating)?.updated

    private val hasLocalWork: Boolean
        get() = debounceJob != null ||
            _saveState.value.isMutating() ||
            _saveState.value.isFailure() ||
            _draft.value != Draft.of(saved)

    private fun setDraft(newDraft: Draft) {
        if (!isEditable || !newDraft.home.isValidScore() || !newDraft.away.isValidScore()) return
        if (newDraft == _draft.value) return
        _draft.value = newDraft
        clearFailure()
        scheduleSave()
    }

    private fun scheduleSave() {
        cancelDebounce()
        // An in-flight save of these scores already covers them; otherwise the newest saved value
        // is the one to differ from.
        val score = _draft.value.complete ?: return
        if (SavedBet.Placed(score) == (inFlight ?: saved)) return
        debounceJob = scope.launch {
            delay(SAVE_DEBOUNCE)
            debounceElapsed()
        }
    }

    private fun cancelDebounce() {
        debounceJob?.cancel()
        debounceJob = null
    }

    private fun debounceElapsed() {
        debounceJob = null
        if (_saveState.value.isMutating()) {
            pendingResave = true
            return
        }
        val score = _draft.value.complete ?: return
        if (SavedBet.Placed(score) == saved) return
        submit(score)
    }

    private fun submit(score: TeamScore<Int>) {
        val wasFailure = _saveState.value.isFailure()
        _saveState.value = MutationState.Mutating(original = saved, updated = SavedBet.Placed(score))
        if (wasFailure) outcomes.failureCleared(matchId)
        startRequest(score)
    }

    private fun startRequest(score: TeamScore<Int>) {
        val match = _match.value
        // The setters admit only valid scores, so both conversions succeed.
        val bet = Bet(
            poolId = match.poolId,
            gamblerId = match.gamblerId,
            matchId = match.matchId,
            homeTeamBet = BetScore(score.homeTeamValue),
            awayTeamBet = BetScore(score.awayTeamValue),
        )
        scope.launch {
            val result = placeBet.execute(bet = bet)
            finishSubmission(attempted = score, result = result)
        }
    }

    /**
     * A queued resave moves the state from one `Mutating` straight to the next, so the row never
     * leaves saving between a save and its resave. A row the feed locked during the request
     * ignores the result.
     */
    private fun finishSubmission(attempted: TeamScore<Int>, result: Result<PoolGamblerBet>) {
        if (!isEditable) return
        result.fold(
            onSuccess = { updated -> saveSucceeded(updated.toPoolGamblerBetModel()) },
            onFailure = { exception ->
                if (exception is BetException.Forbidden) lockAfterRejection() else saveFailed(attempted, exception)
            },
        )
    }

    private fun saveSucceeded(updated: PoolGamblerBetModel) {
        _match.value = updated
        val accepted = SavedBet.of(updated.betScore)
        val resave = takeQueuedResave(over = accepted)
        if (resave != null) {
            _saveState.value = MutationState.Mutating(original = accepted, updated = SavedBet.Placed(resave))
            startRequest(resave)
        } else {
            _saveState.value = MutationState.Mutated(
                original = _saveState.value.activeValue(),
                updated = accepted,
            )
        }
        outcomes.saved(updated.matchId)
    }

    private fun saveFailed(attempted: TeamScore<Int>, exception: Throwable) {
        val original = _saveState.value.activeValue()
        val resave = takeQueuedResave(over = original)
        if (resave != null) {
            _saveState.value = MutationState.Mutating(original = original, updated = SavedBet.Placed(resave))
            startRequest(resave)
            return
        }
        // A draft edited during the flight supersedes the failed scores: it saves itself or shows
        // as incomplete.
        if (_draft.value.complete != attempted) {
            _saveState.value = MutationState.Idle(original)
            return
        }
        _saveState.value = MutationState.Failure(
            original = original,
            updated = SavedBet.Placed(attempted),
            exception = exception.orDefaultLocalized(),
        )
        outcomes.failed(matchId)
    }

    /** Takes the queued resave, if any: the newest complete draft when it differs from [over]. */
    private fun takeQueuedResave(over: SavedBet): TeamScore<Int>? {
        if (!pendingResave) return null
        pendingResave = false
        return _draft.value.complete?.takeIf { score -> SavedBet.Placed(score) != over }
    }

    /** The refused scores are dropped: the row shows the last saved prediction, read-only. */
    private fun lockAfterRejection() {
        adoptLocked(_match.value.copy(isLocked = true, betScore = saved.scoreOrNull()))
        outcomes.locked(matchId)
    }

    /**
     * Makes the row read-only with [model]'s prediction, dropping any draft and queued save.
     * StateFlow skips equal values, so only what changes is emitted.
     */
    private fun adoptLocked(model: PoolGamblerBetModel) {
        cancelDebounce()
        pendingResave = false
        _match.value = model
        val lockedBet = SavedBet.of(model.betScore)
        _saveState.value = MutationState.Idle(lockedBet)
        _draft.value = Draft.of(lockedBet)
    }

    private fun clearFailure() {
        if (!_saveState.value.isFailure()) return
        _saveState.value = MutationState.Idle(_saveState.value.activeValue())
        outcomes.failureCleared(matchId)
    }

    companion object {
        val SAVE_DEBOUNCE = 700.milliseconds

        /**
         * The card's state. The debounce window shows no state of its own: until the request
         * starts, a complete draft reads as the prediction the card already has, saved or not.
         */
        fun rowStateOf(
            match: PoolGamblerBetModel,
            draft: Draft,
            saveState: MutationState<SavedBet>,
        ): RowState = when {
            match.isLocked -> RowState.LOCKED
            saveState.isMutating() -> RowState.SAVING
            saveState.isFailure() -> RowState.FAILED
            draft.isIncomplete -> RowState.INCOMPLETE
            else -> when (saveState.activeValue()) {
                SavedBet.None -> RowState.EMPTY
                is SavedBet.Placed -> RowState.SAVED
            }
        }

        private fun firstStep(team: Team, delta: Int): Draft = when {
            delta < 0 -> Draft(home = 0, away = 0)
            team == Team.HOME -> Draft(home = 1, away = 0)
            else -> Draft(home = 0, away = 1)
        }

        private fun Int?.isValidScore() = this == null || BetScore.isValid(this)
    }
}
