package com.pipel.core.type

data class Email(val value: String) {

    init {
        checkEmpty()
        checkEmailPattern()
    }

    private fun checkEmpty() {
        if (value.isEmpty()) {
            throw IllegalArgumentException("Value must be not empty")
        }
    }

    private fun checkEmailPattern() {
        val pattern = Regex("^([^.@]+)(\\.[^.@]+)*@([^.@]+\\.)+([^.@]+)\$")
        if (!pattern.containsMatchIn(value)) {
            throw IllegalArgumentException("Value must be a valid email")
        }
    }

    override fun toString(): String = value

}