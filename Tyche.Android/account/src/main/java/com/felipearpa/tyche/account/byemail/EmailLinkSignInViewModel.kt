package com.felipearpa.tyche.account.byemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.authentication.application.SignInWithEmailLinkUseCase
import com.felipearpa.tyche.ui.exception.mapOrDefaultLocalized
import com.felipearpa.ui.state.LoadableViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailLinkSignInViewModel @Inject constructor(
    private val signInLinkToEmailUseCase: SignInWithEmailLinkUseCase,
) : ViewModel() {
    private val _state =
        MutableStateFlow<LoadableViewState<AccountBundle>>(LoadableViewState.Initial)
    val state = _state.asStateFlow()

    fun reset() {
        _state.value = LoadableViewState.Initial
    }

    fun signInWithEmailLink(email: String, emailLink: String) {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Loading)
            signInLinkToEmailUseCase.execute(email = Email(email), emailLink = emailLink)
                .onSuccess { accountBundle -> _state.emit(LoadableViewState.Success(accountBundle)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadableViewState.Failure(
                            exception.mapOrDefaultLocalized { it.asEmailLinkSignInLocalizedException() },
                        ),
                    )
                }
        }
    }
}
