package com.felipearpa.tyche.session.authentication.infrastructure

import com.felipearpa.tyche.session.authentication.domain.SignInWithEmailLinkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

suspend fun <Value> handleFirebaseSignInWithEmail(block: suspend () -> Value): Result<Value> {
    return try {
        Result.success(block())
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}

suspend fun <Value> handleFirebaseSignInWithEmailLink(block: suspend () -> Value): Result<Value> {
    return try {
        Result.success(block())
    } catch (_: FirebaseAuthInvalidCredentialsException) {
        Result.failure(SignInWithEmailLinkException.InvalidEmailLink)
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}