package com.felipearpa.bet.domain

sealed class BetException : Throwable() {

    object Forbidden : BetException()
}