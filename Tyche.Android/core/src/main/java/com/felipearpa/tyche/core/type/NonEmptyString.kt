package com.felipearpa.tyche.core.type

@JvmInline
value class NonEmptyString(val value: String) {

    init {
        require(value.isNotBlank()) { "String must be not empty" }
    }

    override fun toString(): String = value
}