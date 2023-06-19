package com.felipearpa.tyche.user.account.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.felipearpa.tyche.ui.LocalizedException
import com.felipearpa.tyche.user.R
import com.felipearpa.tyche.user.account.domain.UserCreationException

sealed class UserCreationLocalizedException : LocalizedException() {

    object UserAlreadyRegistered : UserCreationLocalizedException()

    override val errorDescription: String?
        @Composable get() =
            when (this) {
                UserAlreadyRegistered ->
                    stringResource(id = R.string.user_already_registered_failure_title)
            }

    override val failureReason: String?
        @Composable get() =
            when (this) {
                UserAlreadyRegistered ->
                    stringResource(id = R.string.user_already_registered_failure_message)
            }
}

fun UserCreationException.toUserCreationLocalizedException() =
    when (this) {
        UserCreationException.UserAlreadyRegistered -> UserCreationLocalizedException.UserAlreadyRegistered
    }