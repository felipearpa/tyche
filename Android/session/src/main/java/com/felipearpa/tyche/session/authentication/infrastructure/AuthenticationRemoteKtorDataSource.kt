package com.felipearpa.tyche.session.authentication.infrastructure

import com.felipearpa.tyche.session.authentication.domain.AuthenticationDataSource
import com.felipearpa.tyche.session.authentication.domain.LinkAccountRequest
import com.felipearpa.tyche.session.authentication.domain.LinkAccountResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class AuthenticationRemoteKtorDataSource(private val httpClient: HttpClient) :
    AuthenticationDataSource {
    override suspend fun linkAccount(request: LinkAccountRequest): LinkAccountResponse =
        httpClient.post("accounts") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
