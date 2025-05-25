package com.felipearpa.tyche.account.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.core.type.Email
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.authentication.application.SignInWithEmailAndPasswordUseCase
import com.felipearpa.tyche.ui.exception.orDefaultLocalized
import com.felipearpa.ui.state.LoadableViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailAndPasswordSignInViewModel @Inject constructor(
    private val signInWithEmailAndPasswordUseCase: SignInWithEmailAndPasswordUseCase
) : ViewModel() {
    private val _state =
        MutableStateFlow<LoadableViewState<AccountBundle>>(LoadableViewState.Initial)
    val state = _state.asStateFlow()

    fun reset() {
        _state.value = LoadableViewState.Initial
    }

    fun signInWithEmailAndPassword(email: String, password: String) {
        viewModelScope.launch {
            _state.emit(LoadableViewState.Loading)
            signInWithEmailAndPasswordUseCase.execute(email = Email(email), password = password)
                .onSuccess { accountBundle -> _state.emit(LoadableViewState.Success(accountBundle)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadableViewState.Failure(
                            exception
                                .asEmailAndPasswordSignInLocalized()
                                .orDefaultLocalized()
                        )
                    )
                }
        }
    }
}
