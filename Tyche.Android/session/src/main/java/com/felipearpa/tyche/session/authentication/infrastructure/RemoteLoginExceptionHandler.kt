package com.felipearpa.tyche.session.authentication.infrastructure

import com.felipearpa.tyche.session.authentication.domain.LoginException
import com.felipearpa.tyche.session.authentication.domain.LoginResponse
import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverHttpException

internal suspend fun NetworkExceptionHandler.handleRemoteLogin(block: suspend () -> LoginResponse) =
    this.handle {
        block()
    }.recoverHttpException { exception ->
        when (exception.httpStatusCode) {
            HttpStatusCode.CONFLICT -> LoginException.InvalidCredential
            else -> exception
        }
    }