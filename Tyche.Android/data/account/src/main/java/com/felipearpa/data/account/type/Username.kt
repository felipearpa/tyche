package com.felipearpa.data.account.type

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

        fun isValid(value: String): Boolean {
            return try {
                checkEmpty(value)
                checkUsernamePattern(value)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }

        private fun checkEmpty(value: String) {
            require(value.isNotEmpty()) { "Value must be not empty" }
        }

        private fun checkUsernamePattern(value: String) {
            val pattern = Regex(usernameRegularExpression)
            require(pattern.containsMatchIn(value)) { "Value must be a valid username" }
        }
    }
}