package com.pipel.core

interface PagingQuery<TModel : Any> {

    suspend fun execute(nextToken: String?): CursorPage<TModel>

}