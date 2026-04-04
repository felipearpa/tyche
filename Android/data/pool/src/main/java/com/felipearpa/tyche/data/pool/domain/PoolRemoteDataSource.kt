package com.felipearpa.tyche.data.pool.domain

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class PoolRemoteDataSource(private val httpClient: HttpClient) {
    suspend fun getPool(id: String): PoolResponse =
        httpClient.get("pools/$id").body()

    suspend fun createPool(createPoolRequest: CreatePoolRequest): CreatePoolResponse =
        httpClient.post("pools") {
            contentType(ContentType.Application.Json)
            setBody(createPoolRequest)
        }.body()

    suspend fun joinPool(poolId: String, joinPoolRequest: JoinPoolRequest) {
        httpClient.post("pools/$poolId/gamblers") {
            contentType(ContentType.Application.Json)
            setBody(joinPoolRequest)
        }
    }
}
