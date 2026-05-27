package com.felipearpa.tyche.pool.managegamblers

import com.felipearpa.tyche.data.pool.domain.PoolMember
import java.util.UUID

data class PoolMemberModel(
    val gamblerId: String,
    val gamblerUsername: String,
    val gamblerEmail: String,
    val isOwner: Boolean = false,
)

fun PoolMember.toPoolMemberModel() =
    PoolMemberModel(
        gamblerId = this.gamblerId,
        gamblerUsername = this.gamblerUsername,
        gamblerEmail = this.gamblerEmail,
        isOwner = this.isOwner,
    )

fun poolMemberPlaceholderModel() =
    PoolMemberModel(
        gamblerId = UUID.randomUUID().toString(),
        gamblerUsername = "placeholder",
        gamblerEmail = "placeholder@example.com",
    )
