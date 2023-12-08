package com.felipearpa.tyche.session.managment.infrastructure

import com.felipearpa.tyche.session.managment.domain.AccountCreationException
import com.felipearpa.tyche.session.managment.domain.AccountCreationResponse
import com.felipearpa.tyche.core.network.HttpStatusCode
import com.felipearpa.tyche.core.network.NetworkExceptionHandler
import com.felipearpa.tyche.core.network.recoverHttpException

internal suspend fun NetworkExceptionHandler.handleRemoteAccountCreation(
    block: suspend () -> AccountCreationResponse
) =
    handle {
        block()
    }.recoverHttpException { exception ->
        when (exception.httpStatusCode) {
            HttpStatusCode.BAD_REQUEST -> AccountCreationException.AccountAlreadyRegistered
            else -> exception
        }
    }
