package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Represents options that can be applied to a streaming request.
 *
 * @property includeOptions If set, an additional chunk will be streamed before the `data: [DONE]` message.
 * The `usage` field on this chunk shows the token usage statistics for the entire request,
 * and the `choices` field will always be an empty array. All other chunks will also include a `usage` field,
 * but with a null value.
 */
@Serializable
public class StreamOptions(
    public val includeOptions: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StreamOptions) return false
        return includeOptions == other.includeOptions
    }

    override fun hashCode(): Int {
        return includeOptions.hashCode()
    }

    override fun toString(): String =
        "StreamOptions(includeOptions=$includeOptions)"
}