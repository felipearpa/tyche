package com.felipearpa.tyche.account.byemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.authentication.application.SignInWithEmailLink
import com.felipearpa.tyche.ui.exception.mapOrDefaultLocalized
import com.felipearpa.ui.state.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmailLinkSignInViewModel(
    private val signInLinkToEmail: SignInWithEmailLink,
) : ViewModel() {
    private val _state =
        MutableStateFlow<LoadState<AccountBundle>>(LoadState.Idle)
    val state = _state.asStateFlow()

    fun reset() {
        _state.value = LoadState.Idle
    }

    fun signInWithEmailLink(email: String, emailLink: String) {
        viewModelScope.launch {
            _state.emit(LoadState.Loading)
            signInLinkToEmail.execute(email = Email(email), emailLink = emailLink)
                .onSuccess { accountBundle -> _state.emit(LoadState.Loaded(accountBundle)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadState.Failure(
                            exception.mapOrDefaultLocalized { it.asEmailLinkSignInLocalizedException() },
                        ),
                    )
                }
        }
    }
}
