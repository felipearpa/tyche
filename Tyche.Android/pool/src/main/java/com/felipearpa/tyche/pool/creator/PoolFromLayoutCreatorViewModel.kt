package com.felipearpa.tyche.pool.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.data.pool.application.CreatePoolUseCase
import com.felipearpa.ui.state.EditableViewState
import com.felipearpa.ui.state.relevantValue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PoolFromLayoutCreatorViewModel @AssistedInject constructor(
    private val createPoolUseCase: CreatePoolUseCase,
    @Assisted("gamblerId") private val gamblerId: String,
) : ViewModel() {
    private val _state = MutableStateFlow<EditableViewState<CreatePoolModel>>(
        EditableViewState.Initial(emptyCreatePoolModel()),
    )
    val state = _state.asStateFlow()

    fun createPool(createPoolModel: CreatePoolModel) {
        val currentCreatePoolModel = _state.value.relevantValue()
        _state.value =
            EditableViewState.Loading(current = currentCreatePoolModel, target = createPoolModel)
        viewModelScope.launch {
            val result =
                createPoolUseCase.execute(
                    createPoolInput = createPoolModel.toCreatePoolInput(
                        ownerGamblerId = gamblerId,
                    ),
                )
            result.onSuccess { createPoolOutput ->
                _state.value = EditableViewState.Success(
                    old = currentCreatePoolModel,
                    succeeded = createPoolModel.copy(poolId = createPoolOutput.poolId),
                )
            }.onFailure {
                _state.value = EditableViewState.Failure(
                    current = currentCreatePoolModel,
                    failed = createPoolModel,
                    exception = it,
                )
            }
        }
    }

    fun reset() {
        _state.update { EditableViewState.Initial(it.relevantValue()) }
    }
}
