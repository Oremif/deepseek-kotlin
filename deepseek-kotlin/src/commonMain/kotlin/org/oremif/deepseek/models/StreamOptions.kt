package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Represents options that can be applied to a streaming request.
 *
 * @property includeUsage If set, an additional chunk will be streamed before the `data: [DONE]` message.
 * The `usage` field on this chunk shows the token usage statistics for the entire request,
 * and the `choices` field will always be an empty array. All other chunks will also include a `usage` field,
 * but with a null value.
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
