package com.felipearpa.tyche.session.authentication.infrastructure

import com.felipearpa.tyche.session.authentication.domain.LoginException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

suspend fun handleFirebaseLogin(block: suspend () -> String): Result<String> {
    return try {
        Result.success(block())
    } catch (exception: FirebaseAuthInvalidCredentialsException) {
        Result.failure(LoginException.InvalidCredential)
    } catch (exception: FirebaseAuthInvalidUserException) {
        Result.failure(LoginException.InvalidCredential)
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}