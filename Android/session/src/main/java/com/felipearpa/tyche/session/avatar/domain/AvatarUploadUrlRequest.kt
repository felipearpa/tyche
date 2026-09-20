package com.felipearpa.tyche.session.avatar.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class AvatarUploadUrlRequest(
    val contentLength: Long,
)
