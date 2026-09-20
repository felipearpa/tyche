package com.felipearpa.tyche.poolscore.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.session.CurrentAccountCoordinator
import com.felipearpa.tyche.session.authentication.application.LogOut
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DrawerViewModel(
    private val logOut: LogOut,
    currentAccountCoordinator: CurrentAccountCoordinator,
) : ViewModel() {
    val accountId: StateFlow<String> = currentAccountCoordinator.state
        .map { bundle -> bundle?.accountId.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = currentAccountCoordinator.state.value?.accountId.orEmpty(),
        )

    val email: StateFlow<String> = currentAccountCoordinator.state
        .map { bundle -> bundle?.email.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = currentAccountCoordinator.state.value?.email.orEmpty(),
        )

    val username: StateFlow<String> = currentAccountCoordinator.state
        .map { bundle -> bundle?.username.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = currentAccountCoordinator.state.value?.username.orEmpty(),
        )

    val uiState: StateFlow<PoolScoreListDrawerUiState> = combine(
        accountId,
        email,
        username,
    ) { accountId, email, username ->
        PoolScoreListDrawerUiState(
            accountId = accountId,
            email = email,
            username = username,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PoolScoreListDrawerUiState(
            accountId = accountId.value,
            email = email.value,
            username = username.value,
        ),
    )

    fun logout() {
        viewModelScope.launch {
            logOut.execute()
        }
    }
}
