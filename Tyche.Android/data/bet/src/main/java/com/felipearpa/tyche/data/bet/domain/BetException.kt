package com.felipearpa.tyche.data.bet.domain

sealed class BetException : Throwable() {
    data object Forbidden : BetException()
}
