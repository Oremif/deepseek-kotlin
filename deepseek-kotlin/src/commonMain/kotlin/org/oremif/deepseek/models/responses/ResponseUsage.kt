package org.oremif.deepseek.models.responses

import kotlinx.serialization.Serializable

/**
 * Token usage statistics of a [ModelResponse].
 *
 * The Responses API counts tokens under different names than `/chat/completions`: `input` and
 * `output` rather than `prompt` and `completion`, with the cache hit reported inside
 * [inputTokensDetails] instead of as a sibling field.
 *
 * @property inputTokens Number of input tokens.
 * @property inputTokensDetails Breakdown of [inputTokens], in particular the context cache hit.
 * @property outputTokens Number of output tokens, reasoning tokens included.
 * @property outputTokensDetails Breakdown of [outputTokens], in particular the reasoning tokens.
 * @property totalTokens Total tokens billed for the request ([inputTokens] + [outputTokens]).
 */
@Serializable
public class ResponseUsage(
    public val inputTokens: Int,
    public val inputTokensDetails: InputTokensDetails? = null,
    public val outputTokens: Int,
    public val outputTokensDetails: OutputTokensDetails? = null,
    public val totalTokens: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResponseUsage) return false
        return inputTokens == other.inputTokens &&
            inputTokensDetails == other.inputTokensDetails &&
            outputTokens == other.outputTokens &&
            outputTokensDetails == other.outputTokensDetails &&
            totalTokens == other.totalTokens
    }

    override fun hashCode(): Int {
        var result = inputTokens
        result = 31 * result + inputTokensDetails.hashCode()
        result = 31 * result + outputTokens
        result = 31 * result + outputTokensDetails.hashCode()
        result = 31 * result + totalTokens
        return result
    }

    override fun toString(): String =
        "ResponseUsage(inputTokens=$inputTokens, inputTokensDetails=$inputTokensDetails, " +
            "outputTokens=$outputTokens, outputTokensDetails=$outputTokensDetails, totalTokens=$totalTokens)"
}

/**
 * Breakdown of the input tokens counted in [ResponseUsage.inputTokens].
 *
 * @property cachedTokens Number of input tokens served from the context cache.
 */
@Serializable
public class InputTokensDetails(public val cachedTokens: Int? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InputTokensDetails) return false
        return cachedTokens == other.cachedTokens
    }

    override fun hashCode(): Int = cachedTokens?.hashCode() ?: 0

    override fun toString(): String = "InputTokensDetails(cachedTokens=$cachedTokens)"
}

/**
 * Breakdown of the output tokens counted in [ResponseUsage.outputTokens].
 *
 * @property reasoningTokens Number of chain-of-thought tokens the model produced in thinking mode.
 */
@Serializable
public class OutputTokensDetails(public val reasoningTokens: Int? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OutputTokensDetails) return false
        return reasoningTokens == other.reasoningTokens
    }

    override fun hashCode(): Int = reasoningTokens?.hashCode() ?: 0

    override fun toString(): String = "OutputTokensDetails(reasoningTokens=$reasoningTokens)"
}
