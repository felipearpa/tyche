package com.felipearpa.tyche.core

fun interface IosBundleIdProvider {
    operator fun invoke(): String
}
