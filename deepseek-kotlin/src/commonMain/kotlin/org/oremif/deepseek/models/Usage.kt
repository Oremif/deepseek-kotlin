package org.oremif.deepseek.models

import kotlinx.serialization.Serializable


/**
 * Token usage statistics for a single chat or FIM request.
 *
 * For streamed responses the statistics ride on the **last content chunk** — the one whose
 * single choice carries no new content and a non-null `finish_reason` — instead of a
 * separate usage-only chunk; see [ChatCompletionChunk.usage] and [StreamOptions].
 *
 * @property completionTokens Number of tokens in the generated completion.
 * @property promptTokens Number of tokens in the prompt, equal to [promptCacheHitTokens] +
 * [promptCacheMissTokens].
 * @property promptCacheHitTokens Number of prompt tokens served from the context cache,
 * or `null` when caching does not apply.
 * @property promptCacheMissTokens Number of prompt tokens not served from the context
 * cache, or `null` when caching does not apply.
 * @property promptTokensDetails Breakdown of [promptTokens] under the OpenAI-compatible
 * `prompt_tokens_details` key. `null` unless the server sends that key, which current
 * responses do not — read [promptCacheHitTokens] / [promptCacheMissTokens] instead.
 * @property totalTokens Total tokens billed for the request (prompt + completion).
 * @property completionTokensDetails Breakdown of how [completionTokens] was spent — in
 * particular the tokens spent on reasoning in thinking mode.
 */
@Serializable
public class Usage(
    public val completionTokens: Int,
    public val promptTokens: Int,
    public val promptCacheHitTokens: Int? = null,
    public val promptCacheMissTokens: Int? = null,
    public val promptTokensDetails: PromptTokensDetails? = null,
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
                promptTokensDetails == other.promptTokensDetails &&
                totalTokens == other.totalTokens &&
                completionTokensDetails == other.completionTokensDetails
    }

    override fun hashCode(): Int {
        var result = completionTokens.hashCode()
        result = 31 * result + promptTokens.hashCode()
        result = 31 * result + (promptCacheHitTokens ?: 0)
        result = 31 * result + (promptCacheMissTokens ?: 0)
        result = 31 * result + promptTokensDetails.hashCode()
        result = 31 * result + totalTokens.hashCode()
        result = 31 * result + completionTokensDetails.hashCode()
        return result
    }

    override fun toString(): String =
        "Usage(completionTokens=$completionTokens, promptTokens=$promptTokens, promptCacheHitTokens=$promptCacheHitTokens, promptCacheMissTokens=$promptCacheMissTokens, promptTokensDetails=$promptTokensDetails, totalTokens=$totalTokens, completionTokensDetails=$completionTokensDetails)"
}