package com.felipearpa.tyche.session

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(private val authTokenRetriever: AuthTokenRetriever) :
    Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val authToken = runBlocking {
            authTokenRetriever.authToken()
        } ?: return chain.proceed(chain.request())

        val newRequest = chain
            .request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        return chain.proceed(newRequest)
    }
}