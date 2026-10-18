package com.felipearpa.tyche.pool.managegamblers

import com.felipearpa.tyche.data.pool.application.RemoveGambler
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.ui.state.MutationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Receives the outcomes of a [GamblerRowHolder]'s operations, so the holder does not depend on the
 * screen that keeps it.
 */
interface GamblerRowOutcomes {
    /** The gambler was removed from the pool. */
    fun removed(gamblerId: String)

    /** The gambler's removal failed and the row now shows the failure. */
    fun failed(gamblerId: String)

    /**
     * The row left the failure, either by a retry (the holder is removing again) or by a dismissal
     * (the holder is idle and has no work left).
     */
    fun failureCleared(gamblerId: String)
}

/**
 * The operations of one gambler's row, kept by the screen's view model so they survive the row
 * scrolling away and configuration changes. Requests run on [scope], the screen view model's
 * scope. A started removal is never cancelled by the row; it always ends in
 * [MutationState.Mutated] or [MutationState.Failure] and is reported to [outcomes].
 */
class GamblerRowHolder(
    val member: PoolMemberModel,
    private val poolId: String,
    private val removeGambler: RemoveGambler,
    private val scope: CoroutineScope,
    private val outcomes: GamblerRowOutcomes,
) {

    private val _removal = MutableStateFlow<MutationState<PoolMemberModel>>(MutationState.Idle(member))
    val removal: StateFlow<MutationState<PoolMemberModel>> = _removal.asStateFlow()

    /** Starts the removal unless one is already running or done. From a failure it restarts it. */
    fun remove() {
        val current = _removal.value
        if (current is MutationState.Mutating || current is MutationState.Mutated) return
        startRemoval()
    }

    /** Restarts a failed removal without asking for confirmation again. */
    fun retry() {
        if (_removal.value !is MutationState.Failure) return
        startRemoval()
    }

    /** Returns a failed row to idle without sending a request. */
    fun dismissFailure() {
        if (_removal.value !is MutationState.Failure) return
        _removal.value = MutationState.Idle(member)
        outcomes.failureCleared(member.gamblerId)
    }

    private fun startRemoval() {
        val wasFailure = _removal.value is MutationState.Failure
        _removal.value = MutationState.Mutating(original = member, updated = member)
        if (wasFailure) outcomes.failureCleared(member.gamblerId)

        scope.launch {
            removeGambler.execute(poolId = poolId, gamblerId = member.gamblerId)
                .onSuccess {
                    _removal.value = MutationState.Mutated(original = member, updated = member)
                    outcomes.removed(member.gamblerId)
                }
                .onFailure { exception ->
                    // Reported before the state changes, so the screen's failed set never lags
                    // a row that shows the failure.
                    outcomes.failed(member.gamblerId)
                    _removal.value = MutationState.Failure(
                        original = member,
                        updated = member,
                        exception = exception.orDefaultLocalized(),
                    )
                }
        }
    }
}
