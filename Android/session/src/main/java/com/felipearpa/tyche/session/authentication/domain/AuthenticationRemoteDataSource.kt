package com.felipearpa.tyche.session.authentication.domain

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class AuthenticationRemoteDataSource(private val httpClient: HttpClient) {
    suspend fun linkAccount(request: LinkAccountRequest): LinkAccountResponse =
        httpClient.post("accounts") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
