package com.felipearpa.data.bet.domain

sealed class BetException : Throwable() {
    data object Forbidden : BetException()
}