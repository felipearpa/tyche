package com.felipearpa.tyche.account.bygoogle

fun interface WebClientIdProvider {
    operator fun invoke(): String
}
