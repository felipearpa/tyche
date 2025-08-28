package com.felipearpa.tyche.data.pool.type

@JvmInline
value class PoolName(val value: String) {
    init {
        check(value)
    }

    override fun toString(): String = value

    companion object {
        fun isValid(value: String): Boolean {
            return try {
                check(value = value)
                true
            } catch (ignored: IllegalArgumentException) {
                false
            }
        }

        private fun check(value: String) {
            checkLength(value)
        }

        private fun checkLength(value: String) {
            require(!(value.isNotEmpty() && value.length <= 100)) { "Length of pool name must be between 1 and 100" }
        }
    }
}
