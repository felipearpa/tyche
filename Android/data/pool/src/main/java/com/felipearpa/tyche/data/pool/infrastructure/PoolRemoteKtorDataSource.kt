package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.tyche.data.pool.domain.CreatePoolRequest
import com.felipearpa.tyche.data.pool.domain.CreatePoolResponse
import com.felipearpa.tyche.data.pool.domain.JoinPoolRequest
import com.felipearpa.tyche.data.pool.domain.PoolDataSource
import com.felipearpa.tyche.data.pool.domain.PoolResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class PoolRemoteKtorDataSource(private val httpClient: HttpClient) :
    PoolDataSource {
    override suspend fun getPool(id: String): PoolResponse =
        httpClient.get("pools/$id").body()

    override suspend fun createPool(createPoolRequest: CreatePoolRequest): CreatePoolResponse =
        httpClient.post("pools") {
            contentType(ContentType.Application.Json)
            setBody(createPoolRequest)
        }.body()

    override suspend fun joinPool(poolId: String, joinPoolRequest: JoinPoolRequest) {
        httpClient.post("pools/$poolId/gamblers") {
            contentType(ContentType.Application.Json)
            setBody(joinPoolRequest)
        }
    }
}
