package com.felipearpa.tyche.session.avatar.domain

internal interface AvatarDataSource {
    suspend fun issueUploadUrl(
        accountId: String,
        request: AvatarUploadUrlRequest,
    ): AvatarUploadUrlResponse
}
