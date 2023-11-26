package com.felipearpa.tyche.account.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.data.account.AccountBundle
import com.felipearpa.data.account.login.application.LoginUseCase
import com.felipearpa.tyche.ui.LoadableViewState
import com.felipearpa.tyche.ui.toLocalizedException
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
        viewModelScope.launch {
            _state.emit(LoadableViewState.Initial)
        }
    }

    fun login(loginCredential: LoginCredentialModel) {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Loading)
            loginUseCase.execute(loginCredential = loginCredential.toLoginCredential())
                .onSuccess { accountBundle -> _state.emit(LoadableViewState.Success(accountBundle)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadableViewState.Failure(
                            exception.toLoginLocalizedExceptionOnMatch().toLocalizedException()
                        )
                    )
                }
        }
    }
}