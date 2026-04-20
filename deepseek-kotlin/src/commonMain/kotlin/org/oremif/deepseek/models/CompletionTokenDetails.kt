package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Breakdown of how the `completion_tokens` count in [Usage] was spent.
 *
 * Currently only populated for the `deepseek-reasoner` model, which produces separate
 * reasoning output in addition to the visible assistant message.
 *
 * @property reasoningTokens Number of tokens the model produced as reasoning content.
 */
@Serializable
public class CompletionTokenDetails(
    public val reasoningTokens: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompletionTokenDetails) return false
        return reasoningTokens == other.reasoningTokens
    }

    override fun hashCode(): Int {
        return reasoningTokens.hashCode()
    }

    override fun toString(): String =
        "CompletionTokenDetails(reasoningTokens=$reasoningTokens)"
}