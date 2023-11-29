package com.felipearpa.session.login.domain

import retrofit2.http.Body
import retrofit2.http.POST

internal interface LoginRemoteDataSource {
    @POST("user/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse
}