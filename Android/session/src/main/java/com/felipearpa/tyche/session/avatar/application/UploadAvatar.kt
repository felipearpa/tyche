package com.felipearpa.tyche.session.avatar.application

import com.felipearpa.tyche.session.AccountStorage
import com.felipearpa.tyche.session.avatar.domain.AvatarRepository

class UploadAvatar(
    private val avatarRepository: AvatarRepository,
    private val accountStorage: AccountStorage,
) {
    suspend fun execute(imageData: ByteArray): Result<Unit> {
        val bundle = accountStorage.retrieve()
            ?: return Result.failure(IllegalStateException("No account in storage"))

        return avatarRepository
            .issueUploadUrl(accountId = bundle.accountId, contentLength = imageData.size.toLong())
            .fold(
                onSuccess = { url -> avatarRepository.upload(url = url, data = imageData) },
                onFailure = { Result.failure(it) },
            )
    }
}
