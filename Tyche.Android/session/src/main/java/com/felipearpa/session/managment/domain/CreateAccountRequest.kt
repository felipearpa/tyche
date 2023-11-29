package com.felipearpa.session.managment.domain

data class CreateAccountRequest(
    val username: String,
    val password: String
)