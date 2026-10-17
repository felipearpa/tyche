package com.felipearpa.tyche.pool.managegamblers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.data.pool.application.RemoveGambler
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.ui.state.MutationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The removal of one gambler, scoped to that gambler's row so an in-flight request survives the
 * row scrolling away and configuration changes. A started removal is never cancelled by the row;
 * it always ends in [MutationState.Mutated] or [MutationState.Failure], and reports that outcome
 * to [registry].
 */
class RemoveGamblerViewModel(
    private val poolId: String,
    private val member: PoolMemberModel,
    private val removeGambler: RemoveGambler,
    private val registry: RemovalRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow<MutationState<PoolMemberModel>>(MutationState.Idle(member))
    val state: StateFlow<MutationState<PoolMemberModel>> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            registry.failedGamblerIds.collect { failedIds ->
                if (_state.value is MutationState.Failure && member.gamblerId !in failedIds) {
                    _state.value = MutationState.Idle(member)
                }
            }
        }
    }

    /** Starts the removal unless one is already running or done. */
    fun remove() {
        val current = _state.value
        if (current is MutationState.Mutating || current is MutationState.Mutated) return
        startRemoval()
    }

    /** Restarts a failed removal. */
    fun retry() {
        if (_state.value !is MutationState.Failure) return
        startRemoval()
    }

    /** Returns a failed row to idle without sending a request. */
    fun dismissFailure() {
        if (_state.value !is MutationState.Failure) return
        _state.value = MutationState.Idle(member)
        registry.clearFailed(member.gamblerId)
    }

    private fun startRemoval() {
        registry.clearFailed(member.gamblerId)
        _state.value = MutationState.Mutating(original = member, updated = member)

        viewModelScope.launch {
            removeGambler.execute(poolId = poolId, gamblerId = member.gamblerId)
                .onSuccess {
                    _state.value = MutationState.Mutated(original = member, updated = member)
                    registry.markRemoved(member.gamblerId)
                }
                .onFailure { exception ->
                    // Registered before the state changes, so the registry watcher above never
                    // sees a failure that is missing from the registry.
                    registry.markFailed(member.gamblerId)
                    _state.value = MutationState.Failure(
                        original = member,
                        updated = member,
                        exception = exception.orDefaultLocalized(),
                    )
                }
        }
    }
}
