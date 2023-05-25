package com.felipearpa.core.type

@JvmInline
value class NonEmptyString(val value: String) {

    init {
        if (value.isBlank()) {
            throw IllegalArgumentException("UUID string must be not empty")
        }
    }

    override fun toString(): String = value
}