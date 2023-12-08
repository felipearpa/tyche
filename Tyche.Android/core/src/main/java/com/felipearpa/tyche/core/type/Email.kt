package com.felipearpa.tyche.core.type

// ^ and $ are the start and end anchors, respectively, to match the entire string from start to end.
// [A-Za-z0-9._%+-]+ matches one or more of the following characters: uppercase letters, lowercase letters, digits, period (.), underscore (_), percent sign (%), plus sign (+), or hyphen/minus sign (-). This corresponds to the local part of the email address before the @ symbol.
// @ matches the literal @ symbol.
// [A-Za-z0-9.-]+ matches one or more of the following characters: uppercase letters, lowercase letters, digits, period (.), or hyphen/minus sign (-). This corresponds to the domain part of the email address after the @ symbol.
// \. matches the literal . character. It needs to be escaped with a backslash (\) because . has a special meaning in regex.
// [A-Za-z]{2,} matches two or more uppercase or lowercase letters. This corresponds to the top-level domain (TLD) part of the email address (e.g., com, org, net).
private const val PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"

@JvmInline
value class Email(val value: String) {
    init {
        isValid(value)
    }

    override fun toString(): String = value

    companion object {
        fun isValid(value: String): Boolean {
            return try {
                checkEmpty(value)
                checkEmailPattern(value)
                true
            } catch (ignored: IllegalArgumentException) {
                false
            }
        }

        private fun checkEmpty(value: String) {
            require(value.isNotEmpty()) { "Value must be not empty" }
        }

        private fun checkEmailPattern(value: String) {
            val pattern = Regex(PATTERN)
            require(pattern.containsMatchIn(value)) { "Value must be a valid email" }
        }
    }
}