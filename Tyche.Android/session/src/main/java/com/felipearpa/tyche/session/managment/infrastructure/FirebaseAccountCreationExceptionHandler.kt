package com.felipearpa.tyche.session.managment.infrastructure

import com.felipearpa.tyche.session.managment.domain.AccountCreationException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

suspend fun handleFirebaseAccountCreation(block: suspend () -> String): Result<String> {
    return try {
        Result.success(block())
    } catch (exception: FirebaseAuthUserCollisionException) {
        Result.failure(AccountCreationException.AccountAlreadyRegistered)
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}