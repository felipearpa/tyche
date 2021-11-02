package com.pipel.core

interface PagingQuery<TModel : Any> {

    suspend fun execute(skip: Int, take: Int): Page<TModel>

}