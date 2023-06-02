package com.felipearpa.tyche.bet.ui

sealed class BetAppException : Throwable() {

    object Forbidden : BetAppException()
}