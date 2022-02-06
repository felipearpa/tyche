package com.pipel.core

data class CursorPage<TModel : Any>(
    val items: List<TModel>,
    val nextToken: String?
) {

    fun <TOut : Any> map(mapFunc: MapFunc<TModel, TOut>): CursorPage<TOut> {
        return CursorPage(
            items = this.items.map(mapFunc),
            nextToken = this.nextToken
        )
    }

    companion object {
        fun <TModel : Any> empty(): CursorPage<TModel> = CursorPage(
            items = emptyList(),
            nextToken = null
        )
    }

}