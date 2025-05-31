package com.felipearpa.tyche.bet.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.data.bet.application.BetUseCase
import com.felipearpa.tyche.data.bet.domain.Bet
import com.felipearpa.tyche.data.bet.domain.BetException
import com.felipearpa.tyche.bet.PoolGamblerBetModel
import com.felipearpa.tyche.bet.toPoolGamblerBetModel
import com.felipearpa.tyche.core.type.BetScore
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.ui.state.EditableViewState
import com.felipearpa.ui.state.relevantValue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.util.UUID

class PendingBetItemViewModel @AssistedInject constructor(
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
                val currentPoolGamblerBet = currentState.relevantValue()
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
                                    .orDefaultLocalized()
                            )
                        )
                    }
                }
        }
    }

    fun reset() {
        _state.update { currentState ->
            val poolGamblerBet = currentState.relevantValue()
            EditableViewState.Initial(
                poolGamblerBet.copy(
                    instanceId = UUID.randomUUID().toString()
                )
            )
        }
    }
}
