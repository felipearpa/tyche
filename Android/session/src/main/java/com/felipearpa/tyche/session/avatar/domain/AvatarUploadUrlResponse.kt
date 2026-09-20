package com.felipearpa.tyche.session.avatar.domain

import kotlinx.serialization.Serializable

@Serializable
internal data class AvatarUploadUrlResponse(
    val url: String,
)
