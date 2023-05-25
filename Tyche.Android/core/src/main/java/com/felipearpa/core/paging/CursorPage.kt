package com.felipearpa.core.paging

typealias MapFunc<TA, TB> = (TA) -> TB

data class CursorPage<TModel : Any>(
    val items: List<TModel>,
    val next: String?
) {

    fun <TOut : Any> map(mapFunc: MapFunc<TModel, TOut>): CursorPage<TOut> {
        return CursorPage(
            items = this.items.map(mapFunc),
            next = this.next
        )
    }
}

fun <T : Any> emptyCursorPage() = CursorPage(
    items = emptyList<T>(),
    next = null
)