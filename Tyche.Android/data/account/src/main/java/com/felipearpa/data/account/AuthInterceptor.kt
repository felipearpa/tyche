package com.felipearpa.data.account

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(private val loginStorage: AuthStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val currentProfile = runBlocking {
            loginStorage.retrieve()
        }

        val request = chain.request().newBuilder()

        currentProfile?.let { nonNullableCurrentProfile ->
            request.addHeader("Authorization", "Bearer ${nonNullableCurrentProfile.token}")
        }

        return chain.proceed(request.build())
    }
}