package com.felipearpa.tyche.account.byemailandpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.authentication.application.SignInWithEmailAndPassword
import com.felipearpa.tyche.ui.exception.mapOrDefaultLocalized
import com.felipearpa.ui.state.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmailAndPasswordSignInViewModel(
    private val signInWithEmailAndPassword: SignInWithEmailAndPassword,
) : ViewModel() {
    private val _state =
        MutableStateFlow<LoadState<AccountBundle>>(LoadState.Idle)
    val state = _state.asStateFlow()

    fun reset() {
        _state.value = LoadState.Idle
    }

    fun signInWithEmailAndPassword(email: String, password: String) {
        viewModelScope.launch {
            _state.emit(LoadState.Loading)
            signInWithEmailAndPassword.execute(email = Email(email), password = password)
                .onSuccess { accountBundle -> _state.emit(LoadState.Loaded(accountBundle)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadState.Failure(exception.mapOrDefaultLocalized { it.asEmailAndPasswordSignInLocalized() }),
                    )
                }
        }
    }
}
