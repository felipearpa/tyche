package com.felipearpa.tyche.data.bet.domain

sealed class BetException : Throwable() {
    class Forbidden : BetException()
}
