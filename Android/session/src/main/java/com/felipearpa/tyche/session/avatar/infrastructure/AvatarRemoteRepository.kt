package com.felipearpa.tyche.session.avatar.infrastructure

import com.felipearpa.network.NetworkExceptionHandler
import com.felipearpa.tyche.session.avatar.domain.AvatarDataSource
import com.felipearpa.tyche.session.avatar.domain.AvatarRepository
import com.felipearpa.tyche.session.avatar.domain.AvatarUploadDataSource
import com.felipearpa.tyche.session.avatar.domain.AvatarUploadUrlRequest

internal class AvatarRemoteRepository(
    private val avatarDataSource: AvatarDataSource,
    private val avatarUploadDataSource: AvatarUploadDataSource,
    private val networkExceptionHandler: NetworkExceptionHandler,
) : AvatarRepository {
    override suspend fun issueUploadUrl(accountId: String, contentLength: Long): Result<String> =
        networkExceptionHandler.handle {
            avatarDataSource.issueUploadUrl(
                accountId = accountId,
                request = AvatarUploadUrlRequest(contentLength = contentLength),
            ).url
        }

    override suspend fun upload(url: String, data: ByteArray): Result<Unit> =
        networkExceptionHandler.handle {
            avatarUploadDataSource.upload(url = url, data = data)
        }
}
