package com.felipearpa.tyche.session

import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.http.HttpHeaders

/**
 * Logs authenticated requests in full, except for the `Authorization` header, so the bearer token
 * never reaches the logs.
 */
fun LoggingConfig.authenticatedRequestLogging(logger: Logger) {
    this.logger = logger
    level = LogLevel.ALL
    sanitizeHeader { header -> header == HttpHeaders.Authorization }
}
