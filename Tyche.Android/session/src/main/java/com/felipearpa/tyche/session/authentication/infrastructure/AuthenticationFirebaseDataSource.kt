package com.felipearpa.tyche.session.authentication.infrastructure

import com.felipearpa.tyche.session.authentication.domain.AuthenticationExternalDataSource
import com.felipearpa.tyche.session.authentication.domain.ExternalAccountId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.actionCodeSettings
import kotlinx.coroutines.tasks.await

internal class AuthenticationFirebaseDataSource(private val firebaseAuth: FirebaseAuth) :
    AuthenticationExternalDataSource {
    override suspend fun sendSignInLinkToEmail(email: String) {
        val actionCodeSettings = actionCodeSettings {
            url = "https://felipearpa.github.io/tyche/signin/$email"
            handleCodeInApp = true
        }
        firebaseAuth.sendSignInLinkToEmail(email, actionCodeSettings).await()
    }

    override suspend fun signInWithEmailLink(email: String, emailLink: String): ExternalAccountId {
        val authResult = firebaseAuth.signInWithEmailLink(email, emailLink).await()
        return authResult.user!!.uid
    }

    override suspend fun isSignInWithEmailLink(emailLink: String): Boolean =
        firebaseAuth.isSignInWithEmailLink(emailLink)

    override suspend fun logout() {
        firebaseAuth.signOut()
    }
}