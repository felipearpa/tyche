package com.felipearpa.tyche.session.authentication.domain

import retrofit2.http.Body
import retrofit2.http.POST

internal interface AuthenticationRemoteDataSource {
    @POST("user/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse
}