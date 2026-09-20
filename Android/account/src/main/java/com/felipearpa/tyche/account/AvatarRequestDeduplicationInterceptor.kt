package com.felipearpa.tyche.account

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Coalesces concurrent requests that share an explicit memory-cache key into one underlying
 * load, so several surfaces composing the same gambler at the same bucket produce a single
 * fetch. Requests without an explicit key pass through untouched.
 */
class AvatarRequestDeduplicationInterceptor internal constructor(
    scope: CoroutineScope,
) : Interceptor {

    constructor() : this(scope = CoroutineScope(SupervisorJob() + Dispatchers.Default))

    private val registry = InFlightRequestRegistry<ImageResult>(scope = scope)

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val memoryCacheKey = chain.request.memoryCacheKey ?: return chain.proceed()
        return registry.runCoalesced(key = memoryCacheKey) { chain.proceed() }
    }
}
