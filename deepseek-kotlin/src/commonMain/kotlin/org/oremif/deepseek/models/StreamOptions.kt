package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Represents options that can be applied to a streaming request.
 *
 * @property includeUsage If set, every chunk carries a `usage` field: `null` on the
 * intermediate ones, and the token usage statistics for the whole request on the last
 * content chunk — the one whose single choice adds no content and has a non-null
 * `finish_reason`.
 */
@Serializable
public class StreamOptions(
    public val includeUsage: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StreamOptions) return false
        return includeUsage == other.includeUsage
    }

    override fun hashCode(): Int {
        return includeUsage.hashCode()
    }

    override fun toString(): String =
        "StreamOptions(includeUsage=$includeUsage)"
}
