package com.felipearpa.tyche.bet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.bet.application.BetUseCase
import com.felipearpa.tyche.bet.domain.Bet
import com.felipearpa.tyche.bet.domain.BetException
import com.felipearpa.tyche.core.type.BetScore
import com.felipearpa.tyche.core.type.TeamScore
import com.felipearpa.tyche.core.type.Ulid
import com.felipearpa.tyche.ui.UnknownException
import com.felipearpa.tyche.ui.ViewState
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
        MutableStateFlow<ViewState<PoolGamblerBetModel>>(ViewState.Success(poolGamblerBet))
    val state: StateFlow<ViewState<PoolGamblerBetModel>>
        get() = _state.asStateFlow()

    private var lastSuccessBetScore: TeamScore<Int>? = null

    private var lastFailedBetScore: TeamScore<Int>? = null

    fun bet(betScore: TeamScore<Int>) {
        viewModelScope.launch {
            _state.emit(ViewState.Loading)

            lastSuccessBetScore = poolGamblerBet.betScore
            lastFailedBetScore = betScore
            poolGamblerBet.betScore = betScore

            betUseCase.execute(
                Bet(
                    poolId = Ulid(poolGamblerBet.poolId),
                    gamblerId = Ulid(poolGamblerBet.gamblerId),
                    matchId = Ulid(poolGamblerBet.matchId),
                    homeTeamBet = BetScore(betScore.homeTeamValue),
                    awayTeamBet = BetScore(betScore.awayTeamValue)
                )
            ).onSuccess {
                lastSuccessBetScore = null
                lastFailedBetScore = null

                _state.emit(ViewState.Success(poolGamblerBet))
            }.onFailure { exception ->
                if (exception is BetException.Forbidden) {
                    poolGamblerBet.isLockEnforced = true
                }
                _state.emit(ViewState.Failure(mapToAppException(exception = exception)))
            }
        }
    }

    private fun mapToAppException(exception: Throwable): Throwable {
        return when (exception) {
            is BetException.Forbidden -> BetAppException.Forbidden
            else -> UnknownException()
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

        _state.value = ViewState.Success(poolGamblerBet)
    }
}