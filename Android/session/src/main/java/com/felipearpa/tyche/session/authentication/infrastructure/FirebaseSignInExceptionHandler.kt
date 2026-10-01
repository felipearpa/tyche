package com.felipearpa.tyche.session.authentication.infrastructure

import com.felipearpa.tyche.session.authentication.domain.EmailAndPasswordSignInException
import com.felipearpa.tyche.session.authentication.domain.EmailLinkSignInException
import com.felipearpa.tyche.session.authentication.domain.GoogleSignInException
import com.felipearpa.tyche.session.authentication.domain.SendSignInLinkToEmailException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlin.coroutines.cancellation.CancellationException

suspend fun <Value> handleFirebaseSendSignInLinkToEmail(block: suspend () -> Value): Result<Value> {
    return try {
        Result.success(block())
    } catch (_: FirebaseTooManyRequestsException) {
        Result.failure(SendSignInLinkToEmailException.TooManyRequests)
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

/**
 * Coroutine cancellation is rethrown rather than returned, so a sign-in abandoned by its owner
 * never surfaces as a Google sign-in failure. The email helpers above keep their behavior.
 */
suspend fun <Value> handleFirebaseSignInWithGoogle(block: suspend () -> Value): Result<Value> {
    return try {
        Result.success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: FirebaseAuthInvalidCredentialsException) {
        Result.failure(GoogleSignInException.InvalidCredential)
    } catch (_: FirebaseAuthUserCollisionException) {
        Result.failure(GoogleSignInException.AccountExistsWithDifferentCredential)
    } catch (_: FirebaseNetworkException) {
        Result.failure(GoogleSignInException.NetworkError)
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}
