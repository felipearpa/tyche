package com.felipearpa.tyche.session.avatar.domain

internal interface AvatarUploadDataSource {
    suspend fun upload(url: String, data: ByteArray)
}
