package com.felipearpa.tyche.bet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.data.bet.application.BetUseCase
import com.felipearpa.data.bet.domain.Bet
import com.felipearpa.data.bet.domain.BetException
import com.felipearpa.tyche.core.type.BetScore
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.ui.UnknownLocalizedException
import com.felipearpa.tyche.ui.LoadableViewState
import com.felipearpa.tyche.ui.isFailure
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PoolGamblerBetItemViewModel @AssistedInject constructor(
    @Assisted val poolGamblerBet: PoolGamblerBetModel,
    private val betUseCase: BetUseCase
) :
    ViewModel() {

    private val _state =
        MutableStateFlow<LoadableViewState<PoolGamblerBetModel>>(LoadableViewState.Success(poolGamblerBet))
    val state: StateFlow<LoadableViewState<PoolGamblerBetModel>>
        get() = _state.asStateFlow()

    private var lastSuccessBetScore: TeamScore<Int>? = null

    private var lastFailedBetScore: TeamScore<Int>? = null

    fun bet(betScore: TeamScore<Int>) {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Loading)

            lastSuccessBetScore = poolGamblerBet.betScore
            lastFailedBetScore = betScore
            poolGamblerBet.betScore = betScore

            betUseCase.execute(
                Bet(
                    poolId = poolGamblerBet.poolId,
                    gamblerId = poolGamblerBet.gamblerId,
                    matchId = poolGamblerBet.matchId,
                    homeTeamBet = BetScore(betScore.homeTeamValue),
                    awayTeamBet = BetScore(betScore.awayTeamValue)
                )
            ).onSuccess {
                lastSuccessBetScore = null
                lastFailedBetScore = null

                _state.emit(LoadableViewState.Success(poolGamblerBet))
            }.onFailure { exception ->
                if (exception is BetException.Forbidden) {
                    poolGamblerBet.isLockEnforced = true
                }
                _state.emit(LoadableViewState.Failure(exception.toLocalizedException()))
            }
        }
    }

    fun retryBet() {
        if (!_state.value.isFailure()) {
            return
        }

        lastFailedBetScore?.let { nonNullableLastFailedBetScore ->
            bet(betScore = nonNullableLastFailedBetScore)
        }
    }

    fun restoreBet() {
        if (!_state.value.isFailure()) {
            return
        }

        poolGamblerBet.betScore = lastSuccessBetScore

        _state.value = LoadableViewState.Success(poolGamblerBet)
    }
}

private fun Throwable.toLocalizedException() =
    when (this) {
        is BetException.Forbidden -> BetLocalizedException.Forbidden
        else -> UnknownLocalizedException()
    }