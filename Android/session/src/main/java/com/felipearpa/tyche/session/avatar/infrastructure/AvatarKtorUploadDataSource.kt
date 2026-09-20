package com.felipearpa.tyche.session.avatar.infrastructure

import com.felipearpa.tyche.session.avatar.domain.AvatarUpload
import com.felipearpa.tyche.session.avatar.domain.AvatarUploadDataSource
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.ByteArrayContent

/**
 * Uploads to a presigned URL, whose query-string signature is the only authentication —
 * the request must not carry an `Authorization` header, so this uses the unauthenticated client.
 */
internal class AvatarKtorUploadDataSource(private val httpClient: HttpClient) :
    AvatarUploadDataSource {
    override suspend fun upload(url: String, data: ByteArray) {
        httpClient.put(url) {
            header(HttpHeaders.CacheControl, AvatarUpload.CACHE_CONTROL)
            setBody(ByteArrayContent(data, ContentType.Image.JPEG))
        }
    }
}
