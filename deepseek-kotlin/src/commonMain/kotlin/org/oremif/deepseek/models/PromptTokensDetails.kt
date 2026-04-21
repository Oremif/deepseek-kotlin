package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Breakdown of the prompt tokens counted by [Usage], returned by the API under the
 * `prompt_tokens_details` key.
 *
 * Distinct from the older flat fields [Usage.promptCacheHitTokens] /
 * [Usage.promptCacheMissTokens]: both shapes may appear in the same response, and
 * either may be `null` depending on the model and the API version that served the
 * request.
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
