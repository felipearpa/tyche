package com.felipearpa.data.account.type

// * The password must contain at least one digit ((?=.*\d)).
// * The password must contain at least one lowercase letter ((?=.*[a-z])).
// * The password must contain at least one uppercase letter ((?=.*[A-Z])).
// * The password must contain at least one non-word character (i.e. a special character) ((?=.*[^\w\d\s:])).
// * The password must be between 8 and 16 characters in length (([^\s]){8,16}).
private const val passwordRegularExpression =
    "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^\\w\\d\\s:])([^\\s]){8,16}\$"

@JvmInline
value class Password(val value: String) {
    init {
        checkEmpty(value)
        checkPasswordPattern(value)
    }

    override fun toString(): String = value

    companion object {

        fun isValid(value: String): Boolean {
            return try {
                checkEmpty(value)
                checkPasswordPattern(value)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }

        private fun checkEmpty(value: String) {
            require(value.isNotEmpty()) { "Value must be not empty" }
        }

        private fun checkPasswordPattern(value: String) {
            val pattern = Regex(passwordRegularExpression)
            require(pattern.containsMatchIn(value)) { "Value must be a valid password" }
        }
    }
}