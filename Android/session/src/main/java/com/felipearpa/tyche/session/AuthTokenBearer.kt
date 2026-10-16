package com.felipearpa.tyche.session

import io.ktor.client.plugins.auth.AuthConfig
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Sends the retriever's current token on every request and, when a request gets a 401, forces one
 * token refresh and lets Ktor retry that request once.
 *
 * Tokens are not cached by Ktor: the retried request reads the token through [AuthTokenRetriever]
 * again, which returns the refreshed one. When the forced refresh fails or yields no token, the
 * request is not retried and the original 401 surfaces.
 */
fun AuthConfig.authTokenBearer(authTokenRetriever: AuthTokenRetriever) {
    bearer {
        cacheTokens = false
        loadTokens {
            authTokenRetriever.authToken(forceRefresh = false)?.toBearerTokens()
        }
        refreshTokens {
            authTokenRetriever.forcedTokenOrNull()?.toBearerTokens()
        }
    }
}

private suspend fun AuthTokenRetriever.forcedTokenOrNull(): String? =
    try {
        authToken(forceRefresh = true)
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        null
    }

private fun String.toBearerTokens() = BearerTokens(accessToken = this, refreshToken = null)
