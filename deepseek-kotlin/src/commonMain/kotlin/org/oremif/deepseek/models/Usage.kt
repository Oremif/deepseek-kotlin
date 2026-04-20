package org.oremif.deepseek.models

import kotlinx.serialization.Serializable


/**
 * Token usage statistics for a single chat or FIM request.
 *
 * For streamed responses the [ChatCompletionChunk.usage] or [FIMCompletion.usage] field
 * is populated only on the final usage-only chunk, which the API emits when
 * `streamOptions.includeUsage` is set.
 *
 * @property completionTokens Number of tokens in the generated completion.
 * @property promptTokens Number of tokens in the prompt. When context caching applies,
 * this equals [promptCacheHitTokens] + [promptCacheMissTokens].
 * @property promptCacheHitTokens Number of prompt tokens served from the context cache,
 * or `null` when caching does not apply.
 * @property promptCacheMissTokens Number of prompt tokens not served from the context
 * cache, or `null` when caching does not apply.
 * @property totalTokens Total tokens billed for the request (prompt + completion).
 * @property completionTokensDetails Breakdown of how [completionTokens] was spent (e.g.
 * reasoning tokens for `deepseek-reasoner`).
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