package com.felipearpa.tyche.core.type

import com.github.guepardoapps.kulid.ULID

@JvmInline
value class Ulid(val value: String) {

    init {
        require(ULID.isValid(value)) { "ULID string must be valid" }
    }

    override fun toString(): String = value

    companion object {
        fun randomUlid(): Ulid = Ulid(ULID.random())
    }
}