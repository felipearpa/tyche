package com.felipearpa.tyche.data.pool.domain

data class Pool(
    val id: String,
    val name: String,
    val creatorGamblerId: String,
    val gamblerCount: Int? = null,
)
