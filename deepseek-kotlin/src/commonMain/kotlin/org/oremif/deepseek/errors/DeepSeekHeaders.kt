package org.oremif.deepseek.errors

import io.ktor.http.Headers

/**
 * Immutable, Ktor-free snapshot of HTTP response headers attached to a [DeepSeekException].
 *
 * Provided so that error-handling code does not need to depend on `io.ktor.http.Headers`
 * and remains stable across Ktor major versions.
 *
 * Lookups are case-insensitive, matching RFC 7230 semantics. The order of [names] reflects
 * the insertion order of the backing map.
 *
 * Example:
 * ```kotlin
 * try {
 *     client.chat("hello")
 * } catch (e: DeepSeekException.RateLimitException) {
 *     val retryAfterSeconds = e.headers["Retry-After"]?.toLongOrNull()
 *     // honor retryAfterSeconds before retrying
 * }
 * ```
 */
public class DeepSeekHeaders(
    private val entries: Map<String, List<String>>,
) {
    /**
     * Returns the first value associated with [name] using case-insensitive comparison,
     * or `null` if the header is not present.
     */
    public operator fun get(name: String): String? =
        findEntry(name)?.value?.firstOrNull()

    /**
     * Returns all values associated with [name] using case-insensitive comparison,
     * or an empty list if the header is not present.
     */
    public fun getAll(name: String): List<String> =
        findEntry(name)?.value ?: emptyList()

    /** Returns `true` if the header [name] is present (case-insensitive). */
    public operator fun contains(name: String): Boolean = findEntry(name) != null

    /** Returns the set of header names as originally supplied (case preserved). */
    public fun names(): Set<String> = entries.keys

    /** Returns `true` when there are no headers. */
    public fun isEmpty(): Boolean = entries.isEmpty()

    private fun findEntry(name: String): Map.Entry<String, List<String>>? =
        entries.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }

    override fun equals(other: Any?): Boolean =
        this === other || (other is DeepSeekHeaders && entries == other.entries)

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = "DeepSeekHeaders($entries)"

    public companion object {
        /** A [DeepSeekHeaders] instance with no entries. */
        public val Empty: DeepSeekHeaders = DeepSeekHeaders(emptyMap())
    }
}

internal fun Headers.toDeepSeekHeaders(): DeepSeekHeaders {
    val names = names()
    if (names.isEmpty()) return DeepSeekHeaders.Empty
    val map = LinkedHashMap<String, List<String>>(names.size)
    for (name in names) {
        getAll(name)?.let { map[name] = it }
    }
    return DeepSeekHeaders(map)
}
