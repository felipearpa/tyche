package com.felipearpa.tyche.session.authentication.infrastructure

import com.felipearpa.tyche.session.authentication.domain.EmailLinkSignInException
import com.felipearpa.tyche.session.authentication.domain.EmailAndPasswordSignInException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

suspend fun <Value> handleFirebaseSendSignInLinkToEmail(block: suspend () -> Value): Result<Value> {
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
        Result.failure(EmailLinkSignInException.InvalidEmailLink)
    } catch (_: FirebaseAuthActionCodeException) {
        Result.failure(EmailLinkSignInException.InvalidEmailLink)
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}

suspend fun <Value> handleFirebaseSignInWithEmailAndPassword(block: suspend () -> Value): Result<Value> {
    return try {
        Result.success(block())
    } catch (_: FirebaseAuthInvalidCredentialsException) {
        Result.failure(EmailAndPasswordSignInException.InvalidCredentials)
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}
