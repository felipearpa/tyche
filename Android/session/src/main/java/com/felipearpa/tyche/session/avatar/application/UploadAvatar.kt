package com.felipearpa.tyche.session.avatar.application

import com.felipearpa.tyche.session.CurrentAccountCoordinator
import com.felipearpa.tyche.session.avatar.domain.AvatarRepository
import com.felipearpa.tyche.session.avatar.domain.InstallUploadedAvatar

class UploadAvatar(
    private val avatarRepository: AvatarRepository,
    private val currentAccountCoordinator: CurrentAccountCoordinator,
    private val installUploadedAvatar: InstallUploadedAvatar,
) {
    suspend fun execute(imageData: ByteArray): Result<Unit> {
        // Captured before the PUT so a logout completing mid-upload cannot redirect the seed.
        val accountId = currentAccountCoordinator.state.value?.accountId
            ?: return Result.failure(IllegalStateException("No account in storage"))

        return avatarRepository
            .issueUploadUrl(accountId = accountId, contentLength = imageData.size.toLong())
            .fold(
                onSuccess = { url ->
                    avatarRepository
                        .upload(url = url, data = imageData)
                        .onSuccess { installUploadedAvatar(imageData, accountId) }
                },
                onFailure = { Result.failure(it) },
            )
    }
}
