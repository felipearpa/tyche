package com.felipearpa.tyche.ui

data class TextFieldValidation(
    val isValid: (String) -> Boolean,
    val errorMessage: String?,
)
