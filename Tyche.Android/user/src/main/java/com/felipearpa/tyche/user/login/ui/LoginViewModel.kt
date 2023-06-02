package com.felipearpa.tyche.user.login.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.ui.UnknownException
import com.felipearpa.tyche.ui.ViewState
import com.felipearpa.tyche.ui.network.toNetworkAppException
import com.felipearpa.tyche.user.UserProfile
import com.felipearpa.tyche.user.login.application.LoginUseCase
import com.felipearpa.tyche.user.login.domain.LoginException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val loginUseCase: LoginUseCase) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<UserProfile>>(ViewState.Initial)
    val state: Flow<ViewState<UserProfile>>
        get() = _state.asStateFlow()

    fun resetState() {
        viewModelScope.launch {
            _state.emit(ViewState.Initial)
        }
    }

    fun login(user: User) {
        viewModelScope.launch {
            _state.emit(ViewState.Loading)
            loginUseCase.execute(loginInput = user.toLoginCommand())
                .onSuccess { userProfile -> _state.emit(ViewState.Success(userProfile)) }
                .onFailure { exception -> _state.emit(ViewState.Failure(exception.toAppException())) }
        }
    }
}

private fun Throwable.toAppException() =
    when (this) {
        LoginException.InvalidCredentials -> LoginAppException.InvalidCredential
        is NetworkException -> this.toNetworkAppException()
        else -> UnknownException()
    }