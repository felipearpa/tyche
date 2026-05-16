package com.felipearpa.tyche.session.authentication.domain

data class GoogleSignInResult(
    val externalAccountId: ExternalAccountId,
    val email: String,
)
