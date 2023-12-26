package com.felipearpa.tyche.session.authentication.domain

import com.felipearpa.tyche.session.AccountBundle

interface AuthenticationRepository {
    suspend fun sendSignInLinkToEmail(email: String): Result<Unit>
    suspend fun signInWithEmailLink(email: String, emailLink: String): Result<ExternalAccountId>
    suspend fun logout(): Result<Unit>

    suspend fun linkAccount(accountLink: AccountLink): Result<AccountBundle>
}