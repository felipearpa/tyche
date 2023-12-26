package com.felipearpa.tyche.account.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.authentication.application.SendSignInLinkToEmailUseCase
import com.felipearpa.tyche.ui.exception.orLocalizedException
import com.felipearpa.tyche.ui.state.LoadableViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInWithEmailViewModel @Inject constructor(
    private val sendSignInLinkToEmailUseCase: SendSignInLinkToEmailUseCase
) : ViewModel() {
    private val _state =
        MutableStateFlow<LoadableViewState<Unit>>(LoadableViewState.Initial)
    val state = _state.asStateFlow()

    fun reset() {
        _state.value = LoadableViewState.Initial
    }

    fun sendSignInLinkToEmail(email: String) {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Loading)
            sendSignInLinkToEmailUseCase.execute(email = Email(email))
                .onSuccess { _state.emit(LoadableViewState.Success(Unit)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadableViewState.Failure(
                            exception.orLocalizedException()
                        )
                    )
                }
        }
    }
}