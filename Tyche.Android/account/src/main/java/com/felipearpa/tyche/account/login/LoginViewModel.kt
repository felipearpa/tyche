package com.felipearpa.tyche.account.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.authentication.application.LoginUseCase
import com.felipearpa.tyche.ui.exception.orLocalizedException
import com.felipearpa.tyche.ui.state.LoadableViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val loginUseCase: LoginUseCase) : ViewModel() {
    private val _state =
        MutableStateFlow<LoadableViewState<AccountBundle>>(LoadableViewState.Initial)
    val state: Flow<LoadableViewState<AccountBundle>>
        get() = _state.asStateFlow()

    fun reset() {
        _state.value = LoadableViewState.Initial
    }

    fun login(loginCredential: LoginCredentialModel) {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Loading)
            loginUseCase.execute(loginCredential = loginCredential.toLoginCredential())
                .onSuccess { accountBundle -> _state.emit(LoadableViewState.Success(accountBundle)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadableViewState.Failure(
                            exception.toLoginLocalizedException().orLocalizedException()
                        )
                    )
                }
        }
    }
}