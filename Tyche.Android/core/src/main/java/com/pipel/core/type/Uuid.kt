package com.pipel.core.type

import java.util.*

data class Uuid(val value: UUID) {
    override fun toString(): String = value.toString()

    companion object {
        fun randomUUID(): Uuid = Uuid(UUID.randomUUID())
    }
}