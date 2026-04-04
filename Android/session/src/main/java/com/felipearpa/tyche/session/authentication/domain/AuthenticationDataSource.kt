package com.felipearpa.tyche.session.authentication.domain

internal interface AuthenticationDataSource {
    suspend fun linkAccount(request: LinkAccountRequest): LinkAccountResponse
}
