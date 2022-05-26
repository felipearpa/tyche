package com.pipel.core.type

data class PositiveInt(val value: Int) {

    init {
        if (value < 0) {
            throw IllegalArgumentException("Value must be zero or greater")
        }
    }

    override fun toString(): String = value.toString()

}