package com.felipearpa.tyche.account.bygoogle

import com.felipearpa.tyche.session.AccountBundle
import com.felipearpa.tyche.ui.exception.LocalizedException

/**
 * State of the Google-button sign-in attempt, from the first tap through the app-authentication
 * result.
 *
 * A successful attempt is [Authenticated] until the screen hands the account to navigation and
 * reports it with [GoogleSignInViewModel.authenticationHandled], then [Completed], so a recomposed
 * or recreated screen never navigates a second time. Both stay busy because the screen is leaving.
 */
sealed interface GoogleSignInState {
    /** True while an attempt owns the screen's sign-in actions. */
    val isBusy: Boolean

    data object Idle : GoogleSignInState {
        override val isBusy = false
    }

    /** Waiting for Google to return a credential or for the app authentication that follows. */
    data object InProgress : GoogleSignInState {
        override val isBusy = true
    }

    /** App authentication succeeded and the account has not been handed to navigation yet. */
    data class Authenticated(val accountBundle: AccountBundle) : GoogleSignInState {
        override val isBusy = true
    }

    /** The account was handed to navigation; nothing else is delivered for this attempt. */
    data object Completed : GoogleSignInState {
        override val isBusy = true
    }

    data class Failed(val exception: LocalizedException) : GoogleSignInState {
        override val isBusy = false
    }
}
