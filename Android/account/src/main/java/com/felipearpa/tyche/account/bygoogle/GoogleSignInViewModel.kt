package com.felipearpa.tyche.account.bygoogle

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.session.authentication.application.SignInWithGoogle
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException
import com.felipearpa.tyche.ui.exception.mapOrDefaultLocalized
import com.felipearpa.ui.state.LoadableViewState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoogleSignInViewModel(
    private val credentialProvider: GoogleCredentialProvider,
    private val signInWithGoogle: SignInWithGoogle,
) : ViewModel(), DefaultLifecycleObserver {
    private val _state =
        MutableStateFlow<LoadableViewState<AccountBundle>>(LoadableViewState.Initial)
    val state = _state.asStateFlow()

    private var signInJob: Job? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun reset() {
        _state.value = LoadableViewState.Initial
    }

    override fun onStop(owner: LifecycleOwner) {
        cancelSignIn()
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        cancelSignIn()
    }

    private fun cancelSignIn() {
        signInJob?.cancel()
        signInJob = null
    }

    fun signInWithGoogle(context: Context) {
        signInJob?.cancel()
        signInJob = viewModelScope.launch {
            val idTokenResult = credentialProvider.getIdToken(activityContext = context)
            val idToken = idTokenResult.getOrElse { exception ->
                if (exception != GoogleSignInException.Cancelled) {
                    _state.emit(
                        LoadableViewState.Failure(
                            exception.mapOrDefaultLocalized { it.asGoogleSignInLocalized() },
                        ),
                    )
                }
                return@launch
            }

            _state.emit(LoadableViewState.Loading)

            signInWithGoogle.execute(idToken = idToken)
                .onSuccess { accountBundle -> _state.emit(LoadableViewState.Success(accountBundle)) }
                .onFailure { exception ->
                    _state.emit(
                        LoadableViewState.Failure(
                            exception.mapOrDefaultLocalized { it.asGoogleSignInLocalized() },
                        ),
                    )
                }
        }
    }
}
