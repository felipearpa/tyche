package com.felipearpa.tyche.account.byemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.authentication.application.SendSignInLinkToEmail
import com.felipearpa.tyche.ui.exception.mapOrDefaultLocalized
import com.felipearpa.ui.state.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmailSignInViewModel(
    private val sendSignInLinkToEmail: SendSignInLinkToEmail,
) : ViewModel() {
    private val _state =
        MutableStateFlow<LoadState<String>>(LoadState.Idle)
    val state = _state.asStateFlow()

    fun reset() {
        _state.value = LoadState.Idle
    }

    fun sendSignInLinkToEmail(email: String) {
        viewModelScope.launch {
            _state.emit(LoadState.Loading)
            sendSignInLinkToEmail.execute(email = Email(email))
                .onSuccess { _state.emit(LoadState.Loaded(email)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadState.Failure(
                            exception.mapOrDefaultLocalized { it.asSendSignInLinkToEmailLocalizedException() },
                        ),
                    )
                }
        }
    }
}
