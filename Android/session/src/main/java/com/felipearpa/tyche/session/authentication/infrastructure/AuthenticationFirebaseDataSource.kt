package com.felipearpa.tyche.session.authentication.infrastructure

import android.content.Context
import com.felipearpa.tyche.core.IosBundleIdProvider
import com.felipearpa.tyche.session.SignInLinkUrlTemplateProvider
import com.felipearpa.tyche.session.authentication.domain.AuthenticationExternalDataSource
import com.felipearpa.tyche.session.authentication.domain.ExternalAccountId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.actionCodeSettings
import kotlinx.coroutines.tasks.await

internal class AuthenticationFirebaseDataSource(
    private val firebaseAuth: FirebaseAuth,
    private val signInLinkUrlTemplate: SignInLinkUrlTemplateProvider,
    private val iosBundleId: IosBundleIdProvider,
    private val context: Context,
) : AuthenticationExternalDataSource {

    override suspend fun sendSignInLinkToEmail(email: String) {
        val actionCodeSettings = actionCodeSettings {
            url = String.format(signInLinkUrlTemplate(), email)
            handleCodeInApp = true
            setAndroidPackageName(context.packageName, true, null)
            iosBundleId = iosBundleId()
        }
        firebaseAuth.sendSignInLinkToEmail(email, actionCodeSettings).await()
    }

    override suspend fun signInWithEmailLink(email: String, emailLink: String): ExternalAccountId {
        val authResult = firebaseAuth.signInWithEmailLink(email, emailLink).await()
        return authResult.user!!.uid
    }

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): ExternalAccountId {
        val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return authResult.user!!.uid
    }

    override suspend fun isSignInWithEmailLink(emailLink: String): Boolean =
        firebaseAuth.isSignInWithEmailLink(emailLink)

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }
}
