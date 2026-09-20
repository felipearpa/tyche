package com.felipearpa.tyche.account

import coil3.network.HttpException
import coil3.network.NetworkClient
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse

/**
 * Reports to [avatarImageStore] how the avatar origin answers every image request sent through
 * this client, `ETag` included. Coil does not hand response headers to the app, so this is the
 * one place the store can learn which remote photo a decoded variant came from.
 */
fun NetworkClient.reportingAvatarExchanges(avatarImageStore: AvatarImageStore): NetworkClient =
    AvatarOriginNetworkClient(
        delegate = this,
        onReportExchange = avatarImageStore::noteOriginExchange,
    )

internal class AvatarOriginNetworkClient(
    private val delegate: NetworkClient,
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
    private val onReportExchange: (AvatarOriginExchange) -> Unit,
) : NetworkClient {

    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ): T {
        val accountId = AvatarUrl.accountIdOf(request.url)
            ?: return delegate.executeRequest(request, block)

        val requestedAt = nowEpochMillis()
        return delegate.executeRequest(request) { response ->
            // Coil 3.4.0 leaks its disk snapshot when a response it will not cache reaches
            // `block`, and the entry then refuses every later write in this process: each 304
            // becomes a full download and a replaced photo can never be stored. Failing here
            // instead lets the fetcher close the snapshot; it throws this same exception.
            if (!isUsableOriginStatus(response.code)) throw HttpException(response)

            // Reported once Coil has consumed the response, so consumers re-keyed by a replaced
            // photo read a disk entry that is already refreshed.
            block(response).also {
                onReportExchange(
                    AvatarOriginExchange(
                        accountId = accountId,
                        status = response.code,
                        etag = response.headers["ETag"],
                        requestedAtEpochMillis = requestedAt,
                    ),
                )
            }
        }
    }
}
