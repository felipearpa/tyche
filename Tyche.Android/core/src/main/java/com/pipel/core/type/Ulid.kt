package com.pipel.core.type

import com.github.guepardoapps.kulid.ULID

data class Ulid(val value: String) {

    init {
        if (!ULID.isValid(value)) {
            throw IllegalArgumentException("ULID string must be valid")
        }
    }

    override fun toString(): String = value

    companion object {
        fun randomUlid(): Ulid = Ulid(ULID.random())
    }

}