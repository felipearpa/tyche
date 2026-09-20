package com.felipearpa.tyche.session.avatar.domain

interface AvatarRepository {
    suspend fun issueUploadUrl(accountId: String, contentLength: Long): Result<String>
    suspend fun upload(url: String, data: ByteArray): Result<Unit>
}
