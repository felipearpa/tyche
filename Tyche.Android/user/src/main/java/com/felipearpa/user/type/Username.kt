package com.felipearpa.user.type

// * The username may only contain letters, numbers, underscores, and hyphens ([a-zA-Z0-9_-]).
// * The username must be between 3 and 16 characters in length ({3,16}).
private const val usernameRegularExpression = "^[a-zA-Z0-9_-]{3,16}\$"

@JvmInline
value class Username(val value: String) {

    init {
        checkEmpty(value)
        checkUsernamePattern(value)
    }

    override fun toString(): String = value

    companion object {

        fun hasPattern(value: String): Boolean {
            return try {
                checkEmpty(value)
                checkUsernamePattern(value)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }

        private fun checkEmpty(value: String) {
            if (value.isEmpty()) {
                throw IllegalArgumentException("Value must be not empty")
            }
        }

        private fun checkUsernamePattern(value: String) {
            val pattern = Regex(usernameRegularExpression)
            if (!pattern.containsMatchIn(value)) {
                throw IllegalArgumentException("Value must be a valid username")
            }
        }

    }

}