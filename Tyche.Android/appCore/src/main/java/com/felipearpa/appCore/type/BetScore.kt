package com.felipearpa.appCore.type

@JvmInline
value class BetScore(val value: Int) {

    init {
        checkRange()
    }

    private fun checkRange() {
        if (!isValid(value)) {
            throw IllegalArgumentException("Value must be in range [0, 999]")
        }
    }

    override fun toString(): String = value.toString()

    companion object {

        fun isValid(value: Int) = value in 0..999
    }
}