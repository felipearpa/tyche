package com.felipearpa.tyche.core.paging

import kotlinx.serialization.Serializable

@Serializable
data class CursorPage<Value : Any>(
    val items: List<Value>,
    val next: String?,
)

inline fun <Value : Any, Target : Any> CursorPage<Value>.map(transform: (Value) -> Target): CursorPage<Target> =
    CursorPage(
        items = this.items.map(transform),
        next = this.next,
    )

private val emptyCursorPageInstance = CursorPage<Nothing>(
    items = emptyList(),
    next = null,
)

fun <Value : Any> emptyCursorPage() = emptyCursorPageInstance
