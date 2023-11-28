package com.felipearpa.tyche.bet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.data.bet.application.BetUseCase
import com.felipearpa.data.bet.domain.Bet
import com.felipearpa.data.bet.domain.BetException
import com.felipearpa.tyche.core.type.BetScore
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.exception.toLocalizedException
import com.felipearpa.tyche.ui.state.EditableViewState
import com.felipearpa.tyche.ui.state.currentValue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.util.UUID

class PoolGamblerBetItemViewModel @AssistedInject constructor(
    @Assisted poolGamblerBet: PoolGamblerBetModel,
    private val betUseCase: BetUseCase
) :
    ViewModel() {

    private val _state =
        MutableStateFlow<EditableViewState<PoolGamblerBetModel>>(
            EditableViewState.Initial(poolGamblerBet)
        )
    val state = _state.asStateFlow()

    fun retryBet() {
        val poolGamblerBet = when (val stateValue = this.state.value) {
            is EditableViewState.Failure -> stateValue.failed
            else -> return
        }
        val betScore = poolGamblerBet.betScore ?: return
        bet(betScore = betScore)
    }

    fun bet(betScore: TeamScore<Int>) {
        viewModelScope.launch {
            val (currentPoolGamblerBet, targetPoolGamblerBet) = _state.updateAndGet { currentState ->
                val currentPoolGamblerBet = currentState.currentValue()
                val targetPoolGamblerBet = currentPoolGamblerBet.copy(betScore = betScore)
                EditableViewState.Loading(
                    current = currentPoolGamblerBet,
                    target = targetPoolGamblerBet
                )
            } as EditableViewState.Loading

            val bet = Bet(
                poolId = currentPoolGamblerBet.poolId,
                gamblerId = currentPoolGamblerBet.gamblerId,
                matchId = currentPoolGamblerBet.matchId,
                homeTeamBet = BetScore(betScore.homeTeamValue),
                awayTeamBet = BetScore(betScore.awayTeamValue)
            )

            betUseCase.execute(bet = bet)
                .onSuccess { updatedPoolGamblerBet ->
                    _state.emit(
                        EditableViewState.Success(
                            old = currentPoolGamblerBet,
                            succeeded = updatedPoolGamblerBet.toPoolGamblerBetModel()
                        )
                    )
                }.onFailure { exception ->
                    if (exception is BetException.Forbidden) {
                        _state.emit(EditableViewState.Initial(currentPoolGamblerBet.copy(isLocked = true)))
                    } else {
                        _state.emit(
                            EditableViewState.Failure(
                                current = currentPoolGamblerBet,
                                failed = targetPoolGamblerBet,
                                exception = exception
                                    .toBetLocalizedExceptionOnMatch()
                                    .toLocalizedException()
                            )
                        )
                    }
                }
        }
    }

    fun reset() {
        _state.update { currentState ->
            val poolGamblerBet = currentState.currentValue()
            EditableViewState.Initial(
                poolGamblerBet.copy(
                    instanceId = UUID.randomUUID().toString()
                )
            )
        }
    }
}