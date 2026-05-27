package com.felipearpa.tyche.data.pool.domain

internal fun PoolMemberResponse.toPoolMember() =
    PoolMember(
        gamblerId = this.gamblerId,
        gamblerUsername = this.gamblerUsername,
        gamblerEmail = this.gamblerEmail,
        isOwner = this.isOwner,
    )
