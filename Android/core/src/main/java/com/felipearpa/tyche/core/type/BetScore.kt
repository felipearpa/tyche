package com.felipearpa.tyche.core.type

@JvmInline
value class BetScore(val value: Int) {
    init {
        checkRange()
    }

    private fun checkRange() {
        require(isValid(value)) { "Value must be in range [0, 999]" }
    }

    override fun toString(): String = value.toString()

    companion object {
        fun isValid(value: Int) = value in 0..999
    }
}