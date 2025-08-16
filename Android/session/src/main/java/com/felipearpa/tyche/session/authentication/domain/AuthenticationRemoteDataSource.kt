package com.felipearpa.tyche.session.authentication.domain

import retrofit2.http.Body
import retrofit2.http.POST

internal interface AuthenticationRemoteDataSource {
    @POST("accounts")
    suspend fun linkAccount(@Body request: LinkAccountRequest): LinkAccountResponse
}
