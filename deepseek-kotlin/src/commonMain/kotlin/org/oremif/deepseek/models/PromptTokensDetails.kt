package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Breakdown of the prompt tokens counted by [Usage], decoded from the OpenAI-compatible
 * `prompt_tokens_details` key.
 *
 * Current responses omit that key and report cache accounting through the flat
 * [Usage.promptCacheHitTokens] / [Usage.promptCacheMissTokens] fields instead.
 *
 * @property cachedTokens Number of prompt tokens served from the context cache, or
 * `null` when the server did not report this breakdown.
 */
@Serializable
public class PromptTokensDetails(
    public val cachedTokens: Int? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PromptTokensDetails) return false
        return cachedTokens == other.cachedTokens
    }

    override fun hashCode(): Int = cachedTokens?.hashCode() ?: 0

    override fun toString(): String = "PromptTokensDetails(cachedTokens=$cachedTokens)"
}
