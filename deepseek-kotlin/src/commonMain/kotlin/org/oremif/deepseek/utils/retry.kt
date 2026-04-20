package org.oremif.deepseek.utils

import kotlin.random.Random

internal fun isRetryableStatus(statusCode: Int): Boolean =
    statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode in 500..599

internal const val DEFAULT_RETRY_BASE_MILLIS: Long = 500L
internal const val DEFAULT_RETRY_MAX_MILLIS: Long = 30_000L
internal const val DEFAULT_RETRY_JITTER_MILLIS: Long = 250L

/**
 * Exponential backoff with jitter.
 *
 * Delay formula: `min(baseMillis * 2^retry, maxMillis) + Random.nextLong(jitterMillis)`.
 * `retry` is clamped to [1, 30] so `baseMillis shl retry` never overflows `Long`.
 * The [maxMillis] cap also protects against runaway delays when the plugin is configured
 * with a large `maxRetries`.
 */
internal fun computeRetryDelayMillis(
    retry: Int,
    baseMillis: Long = DEFAULT_RETRY_BASE_MILLIS,
    maxMillis: Long = DEFAULT_RETRY_MAX_MILLIS,
    jitterMillis: Long = DEFAULT_RETRY_JITTER_MILLIS,
    random: Random = Random.Default,
): Long {
    val shift = retry.coerceIn(1, 30)
    val exponential = (baseMillis shl shift).coerceAtMost(maxMillis)
    val jitter = if (jitterMillis > 0L) random.nextLong(jitterMillis) else 0L
    return exponential + jitter
}
