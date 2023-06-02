package com.felipearpa.tyche.bet.domain

sealed class BetException : Throwable() {

    object Forbidden : BetException()
}