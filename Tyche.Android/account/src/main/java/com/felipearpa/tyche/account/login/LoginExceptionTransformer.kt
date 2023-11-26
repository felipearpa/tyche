package com.felipearpa.tyche.account.login

import com.felipearpa.data.account.login.domain.LoginException

fun LoginException.toLoginLocalizedException() =
    when (this) {
        LoginException.InvalidCredential -> LoginLocalizedException.InvalidCredential
    }