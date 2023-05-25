package com.felipearpa.bet.ui

sealed class BetAppException : Throwable() {

    object Forbidden : BetAppException()
}