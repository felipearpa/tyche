package com.felipearpa.tyche.account.bygoogle

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipearpa.tyche.session.authentication.application.SignInWithGoogle
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException
import com.felipearpa.tyche.ui.exception.mapOrDefaultLocalized
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs one Google-button sign-in attempt at a time.
 *
 * The attempt belongs to this view model's scope, not to the process lifecycle: Google's account
 * selection, account addition, consent, and verification screens put Fortuna in the background,
 * and the original request must still be waiting when Google returns. The attempt is canceled
 * only when the owning screen is destroyed, and a canceled attempt publishes nothing, so a late
 * result never reaches a departed screen.
 *
 * Cancellation boundary: coroutine cancellation passes through the credential provider and the
 * Google authentication chain unchanged and ends the attempt here without a state change. The
 * gambler canceling Google's screens is not coroutine cancellation; it returns the attempt to
 * [GoogleSignInState.Idle]. Every other outcome, returned or thrown, ends in
 * [GoogleSignInState.Authenticated] or [GoogleSignInState.Failed], never in lasting progress, and
 * nothing is retried automatically: the next attempt is the gambler's next tap. Firebase's own
 * sign-in task can outlive a canceled attempt; that is accepted, and the screen ignores it.
 */
class GoogleSignInViewModel(
    private val credentialProvider: GoogleCredentialProvider,
    private val signInWithGoogle: SignInWithGoogle,
) : ViewModel() {
    private val _state = MutableStateFlow<GoogleSignInState>(GoogleSignInState.Idle)
    val state: StateFlow<GoogleSignInState> = _state.asStateFlow()

    /**
     * Starts an attempt unless one is already busy; repeated actions leave the active request
     * alone instead of restarting it. Starting clears an earlier failure.
     */
    fun signInWithGoogle(activityContext: Context) {
        if (_state.value.isBusy) return
        _state.value = GoogleSignInState.InProgress

        viewModelScope.launch {
            val idToken = settle { credentialProvider.getIdToken(activityContext = activityContext) }
                .getOrElse { exception ->
                    publish(
                        if (exception == GoogleSignInException.Cancelled) {
                            GoogleSignInState.Idle
                        } else {
                            failed(exception)
                        },
                    )
                    return@launch
                }

            settle { signInWithGoogle.execute(idToken = idToken) }
                .onSuccess { accountBundle -> publish(GoogleSignInState.Authenticated(accountBundle)) }
                .onFailure { exception -> publish(failed(exception)) }
        }
    }

    /** Records that the screen handed the authenticated account to navigation. */
    fun authenticationHandled() {
        _state.update { state ->
            if (state is GoogleSignInState.Authenticated) GoogleSignInState.Completed else state
        }
    }

    fun dismissFailure() {
        _state.update { state ->
            if (state is GoogleSignInState.Failed) GoogleSignInState.Idle else state
        }
    }

    /**
     * Turns a thrown failure into a returned one so every attempt ends in a terminal state; the
     * account installation inside [SignInWithGoogle], for one, throws. Cancellation of this
     * attempt is rethrown and publishes nothing. A cancellation exception that arrives while the
     * attempt is still active belongs to some other operation, so it counts as a failure rather
     * than silently ending the attempt with the screen still busy.
     */
    private suspend inline fun <Value> settle(block: () -> Result<Value>): Result<Value> =
        try {
            block()
        } catch (exception: CancellationException) {
            currentCoroutineContext().ensureActive()
            Result.failure(exception)
        } catch (exception: Exception) {
            Result.failure(exception)
        }

    private suspend fun publish(state: GoogleSignInState) {
        currentCoroutineContext().ensureActive()
        _state.value = state
    }

    private fun failed(exception: Throwable) =
        GoogleSignInState.Failed(exception.mapOrDefaultLocalized { it.asGoogleSignInLocalized() })
}
