package com.pipel.core.type

data class NonEmptyString(val value: String) {

    init {
        if (value.isEmpty()) {
            throw IllegalArgumentException("UUID string must be not empty")
        }
    }

    override fun toString(): String = value

}