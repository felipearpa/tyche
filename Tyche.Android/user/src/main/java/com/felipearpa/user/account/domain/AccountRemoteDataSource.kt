package com.felipearpa.user.account.domain

import com.felipearpa.user.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AccountRemoteDataSource {

    @POST("user")
    suspend fun create(@Body createUserRequest: CreateUserRequest): LoginResponse
}