package com.felipearpa.tyche.session.avatar.domain

/**
 * Seeds a just-uploaded avatar into the local image caches so every visible surface shows the
 * new photo without re-downloading it. Implemented outside this module by the shared avatar
 * image store.
 */
fun interface InstallUploadedAvatar {
    suspend operator fun invoke(imageData: ByteArray, accountId: String)
}
