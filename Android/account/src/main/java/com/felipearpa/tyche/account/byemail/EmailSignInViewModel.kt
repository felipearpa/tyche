package com.felipearpa.tyche.account.byemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.authentication.application.SendSignInLinkToEmail
import com.felipearpa.tyche.ui.exception.mapOrDefaultLocalized
import com.felipearpa.ui.state.LoadableViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmailSignInViewModel(
    private val sendSignInLinkToEmail: SendSignInLinkToEmail,
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
            sendSignInLinkToEmail.execute(email = Email(email))
                .onSuccess { _state.emit(LoadableViewState.Success(Unit)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadableViewState.Failure(
                            exception.mapOrDefaultLocalized { it.asSendSignInLinkToEmailLocalizedException() },
                        ),
                    )
                }
        }
    }
}
