package com.felipearpa.tyche.data.pool.infrastructure

import com.felipearpa.tyche.core.paging.CursorPage
import com.felipearpa.tyche.data.pool.domain.PoolMemberDataSource
import com.felipearpa.tyche.data.pool.domain.PoolMemberResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class PoolMemberRemoteKtorDataSource(private val httpClient: HttpClient) :
    PoolMemberDataSource {
    override suspend fun getPoolMembers(
        poolId: String,
        next: String?,
    ): CursorPage<PoolMemberResponse> =
        httpClient.get("pools/$poolId/members") {
            parameter("next", next)
        }.body()

    override suspend fun removeGambler(poolId: String, gamblerId: String) {
        httpClient.delete("pools/$poolId/members/$gamblerId")
    }
}
