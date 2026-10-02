package com.felipearpa.tyche.pool.managegamblers

import com.felipearpa.tyche.data.pool.domain.PoolMember

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

/** Presentation-only filler for loading rows; its fixed values keep redraws stable. */
fun poolMemberPlaceholderModel() =
    PoolMemberModel(
        gamblerId = "placeholder",
        gamblerUsername = "placeholder",
        gamblerEmail = "placeholder@example.com",
    )
