package com.felipearpa.tyche.account

import com.felipearpa.tyche.session.avatar.domain.InstallUploadedAvatar

/**
 * Adapts the shared [AvatarImageStore] to the upload flow's [InstallUploadedAvatar] port, so
 * the use case can seed every visible surface without depending on this module.
 */
class AvatarImageStoreInstallUploadedAvatar(
    private val avatarImageStore: AvatarImageStore,
) : InstallUploadedAvatar {
    override suspend fun invoke(imageData: ByteArray, accountId: String) {
        avatarImageStore.installUploadedAvatar(jpegData = imageData, accountId = accountId)
    }
}
