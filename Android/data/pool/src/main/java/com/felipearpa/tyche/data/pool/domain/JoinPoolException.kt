package com.felipearpa.tyche.data.pool.domain

sealed class JoinPoolException : Throwable() {
    class AlreadyJoined : JoinPoolException()
}
