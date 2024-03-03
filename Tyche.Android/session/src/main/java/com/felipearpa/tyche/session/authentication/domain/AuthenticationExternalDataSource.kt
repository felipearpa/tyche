package com.felipearpa.tyche.session.authentication.domain

typealias ExternalAccountId = String

interface AuthenticationExternalDataSource {
    suspend fun sendSignInLinkToEmail(email: String)
    suspend fun signInWithEmailLink(email: String, emailLink: String): ExternalAccountId
    suspend fun isSignInWithEmailLink(emailLink: String): Boolean
    suspend fun signOut()
}