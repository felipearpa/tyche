package com.felipearpa.tyche.core.paging

typealias TransformFunc<TA, TB> = (TA) -> TB

data class CursorPage<Value : Any>(
    val items: List<Value>,
    val next: String?
) {
    fun <Target : Any> map(transform: TransformFunc<Value, Target>): CursorPage<Target> {
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