package com.felipearpa.tyche.session.managment.domain

import retrofit2.http.Body
import retrofit2.http.POST

internal interface AccountRemoteDataSource {
    @POST("user")
    suspend fun create(@Body accountCreationRequest: AccountCreationRequest): AccountCreationResponse
}