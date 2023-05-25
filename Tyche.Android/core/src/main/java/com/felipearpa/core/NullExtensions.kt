package com.felipearpa.core

inline fun <T> T?.ifNull(block: () -> Unit): T? {
    if (this == null) block()
    return this@ifNull
}

inline fun <T> T?.ifNotNull(block: (T) -> Unit): T? {
    this?.let(block)
    return this@ifNotNull
}