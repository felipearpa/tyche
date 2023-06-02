package com.felipearpa.tyche.user.login.domain

import com.felipearpa.tyche.user.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginRemoteDataSource {

    @POST("user/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse
}