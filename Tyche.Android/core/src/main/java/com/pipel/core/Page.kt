package com.pipel.core

data class Page<TModel : Any>(
    val items: List<TModel>,
    val itemsCount: Int,
    val skip: Int,
    val take: Int,
    val hasNext: Boolean,
) {

    fun <TOut : Any> map(mapFunc: MapFunc<TModel, TOut>): Page<TOut> {
        return Page(
            items = this.items.map(mapFunc),
            itemsCount = this.itemsCount,
            skip = this.skip,
            take = this.take,
            hasNext = this.hasNext
        )
    }

    companion object {
        fun <TModel : Any> empty(): Page<TModel> = Page(
            items = emptyList(),
            itemsCount = 0,
            skip = 0,
            take = 0,
            hasNext = false
        )
    }

}