package org.oremif.deepseek.models

import kotlinx.serialization.Serializable


/**
 * @property completionTokens Number of tokens in the generated completion.
 * @property promptTokens Number of tokens in the prompt. It equals [promptCacheHitTokens] + [promptCacheMissTokens].
 * @property promptCacheHitTokens Number of tokens in the prompt that hits the context cache.
 * @property promptCacheMissTokens Number of tokens in the prompt that misses the context cache.
 * @property totalTokens Total number of tokens used in the request (prompt + completion).
 * @property completionTokensDetails Breakdown of tokens used in a completion.
 */
@Serializable
public class Usage(
    public val completionTokens: Int,
    public val promptTokens: Int,
    public val promptCacheHitTokens: Int? = null,
    public val promptCacheMissTokens: Int? = null,
    public val totalTokens: Int,
    public val completionTokensDetails: CompletionTokenDetails? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Usage) return false
        return completionTokens == other.completionTokens &&
                promptTokens == other.promptTokens &&
                promptCacheHitTokens == other.promptCacheHitTokens &&
                promptCacheMissTokens == other.promptCacheMissTokens &&
                totalTokens == other.totalTokens &&
                completionTokensDetails == other.completionTokensDetails
    }

    override fun hashCode(): Int {
        var result = completionTokens.hashCode()
        result = 31 * result + promptTokens.hashCode()
        result = 31 * result + (promptCacheHitTokens ?: 0)
        result = 31 * result + (promptCacheMissTokens ?: 0)
        result = 31 * result + totalTokens.hashCode()
        result = 31 * result + (completionTokensDetails?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "Usage(completionTokens=$completionTokens, promptTokens=$promptTokens, promptCacheHitTokens=$promptCacheHitTokens, promptCacheMissTokens=$promptCacheMissTokens, totalTokens=$totalTokens, completionTokensDetails=$completionTokensDetails)"
}