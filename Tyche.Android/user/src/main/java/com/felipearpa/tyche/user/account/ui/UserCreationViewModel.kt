package com.felipearpa.tyche.user.account.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.core.network.NetworkException
import com.felipearpa.tyche.ui.UnknownLocalizedException
import com.felipearpa.tyche.ui.ViewState
import com.felipearpa.tyche.ui.network.toNetworkLocalizedException
import com.felipearpa.tyche.user.UserProfile
import com.felipearpa.tyche.user.account.application.CreateUserUseCase
import com.felipearpa.tyche.user.account.application.toCreateUserInput
import com.felipearpa.tyche.user.account.domain.UserCreationException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserCreationViewModel @Inject constructor(
    private val createUserUseCase: CreateUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ViewState<UserProfile>>(ViewState.Initial)
    val state: Flow<ViewState<UserProfile>>
        get() = _state.asStateFlow()

    fun resetState() {
        viewModelScope.launch {
            _state.emit(ViewState.Initial)
        }
    }

    fun create(user: UserModel) {
        viewModelScope.launch {
            _state.emit(ViewState.Loading)
            createUserUseCase.execute(user.toCreateUserInput())
                .onSuccess { userProfile -> _state.emit(ViewState.Success(userProfile)) }
                .onFailure { exception -> _state.emit(ViewState.Failure(exception.toLocalizedException())) }
        }
    }
}

private fun Throwable.toLocalizedException() =
    when (this) {
        is UserCreationException -> this.toUserCreationLocalizedException()
        is NetworkException -> this.toNetworkLocalizedException()
        else -> UnknownLocalizedException()
    }