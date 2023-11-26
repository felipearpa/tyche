package com.felipearpa.tyche.core.paging

typealias MapFunc<TA, TB> = (TA) -> TB

data class CursorPage<Value : Any>(
    val items: List<Value>,
    val next: String?
) {
    fun <Target : Any> map(transform: MapFunc<Value, Target>): CursorPage<Target> {
        return CursorPage(
            items = this.items.map(transform),
            next = this.next
        )
    }
}

fun <Value : Any> emptyCursorPage() = CursorPage(
    items = emptyList<Value>(),
    next = null
)