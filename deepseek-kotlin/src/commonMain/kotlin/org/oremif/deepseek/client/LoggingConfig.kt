package org.oremif.deepseek.client

import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger

/**
 * Configuration for HTTP request/response logging.
 *
 * Obtained via the [DeepSeekClientBase.Builder.logging] DSL. Logging is opt-in — create a
 * [LoggingConfig] by calling `logging { }` on the builder; otherwise nothing is logged.
 *
 * Example:
 * ```kotlin
 * val client = DeepSeekClient("token") {
 *     logging {
 *         level = LogLevel.BODY
 *         sanitizeHeader { header -> header == "Cookie" }
 *     }
 * }
 * ```
 *
 * @property level Log level controlling what is logged. Defaults to [LogLevel.HEADERS].
 * @property logger Destination for log lines. Defaults to [Logger.DEFAULT].
 */
public class LoggingConfig internal constructor() {
    public var level: LogLevel = LogLevel.HEADERS
    public var logger: Logger = Logger.DEFAULT

    internal val sanitizers: MutableList<(String) -> Boolean> = mutableListOf()

    /**
     * Redacts headers whose name satisfies [predicate] from log output.
     *
     * Can be called multiple times to register several predicates — a header is redacted if
     * any predicate returns `true`. The `Authorization` header is always redacted regardless
     * of the configured predicates.
     *
     * @param predicate Returns `true` to redact the header with the given name
     */
    public fun sanitizeHeader(predicate: (String) -> Boolean) {
        sanitizers += predicate
    }
}
