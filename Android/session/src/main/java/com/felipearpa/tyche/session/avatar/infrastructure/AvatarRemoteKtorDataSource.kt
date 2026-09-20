package com.felipearpa.tyche.session.avatar.infrastructure

import com.felipearpa.tyche.session.avatar.domain.AvatarDataSource
import com.felipearpa.tyche.session.avatar.domain.AvatarUploadUrlRequest
import com.felipearpa.tyche.session.avatar.domain.AvatarUploadUrlResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class AvatarRemoteKtorDataSource(private val httpClient: HttpClient) : AvatarDataSource {
    override suspend fun issueUploadUrl(
        accountId: String,
        request: AvatarUploadUrlRequest,
    ): AvatarUploadUrlResponse =
        httpClient.post("accounts/$accountId/avatar-upload-url") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
