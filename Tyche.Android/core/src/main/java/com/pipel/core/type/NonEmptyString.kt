package com.pipel.core.type

data class NonEmptyString(val value: String) {

    init {
        if (value.isEmpty()) {
            throw IllegalArgumentException("")
        }
    }

    override fun toString(): String = value

}