package com.felipearpa.session.managment.domain

import com.felipearpa.session.login.domain.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

internal interface AccountRemoteDataSource {
    @POST("user")
    suspend fun create(@Body createUserRequest: CreateAccountRequest): LoginResponse
}