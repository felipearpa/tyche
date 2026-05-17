package com.felipearpa.tyche.pool.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.data.pool.application.CreatePool
import com.felipearpa.ui.state.MutationState
import com.felipearpa.ui.state.activeValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PoolFromLayoutCreatorViewModel(
    private val createPool: CreatePool,
    private val gamblerId: String,
) : ViewModel() {
    private val _state = MutableStateFlow<MutationState<CreatePoolModel>>(
        MutationState.Idle(emptyCreatePoolModel()),
    )
    val state = _state.asStateFlow()

    fun createPool(createPoolModel: CreatePoolModel) {
        val currentCreatePoolModel = _state.value.activeValue()
        _state.value =
            MutationState.Mutating(original = currentCreatePoolModel, updated = createPoolModel)
        viewModelScope.launch {
            val result =
                createPool.execute(
                    createPoolInput = createPoolModel.toCreatePoolInput(
                        ownerGamblerId = gamblerId,
                    ),
                )
            result.onSuccess { createPoolOutput ->
                _state.value = MutationState.Mutated(
                    original = currentCreatePoolModel,
                    updated = createPoolModel.copy(poolId = createPoolOutput.poolId),
                )
            }.onFailure {
                _state.value = MutationState.Failure(
                    original = currentCreatePoolModel,
                    updated = createPoolModel,
                    exception = it,
                )
            }
        }
    }

    fun reset() {
        _state.update { MutationState.Idle(it.activeValue()) }
    }
}
