package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Log-probability information attached to a [ChatChoice] or [ChatChoiceChunk].
 *
 * Populated only when the request sets `logprobs = true`.
 *
 * @property content Per-token log-probabilities for the assistant's visible content.
 * @property reasoningContent Per-token log-probabilities for the `deepseek-reasoner`
 * reasoning trace, when applicable.
 */
@Serializable
public class LogProbs(
    public val content: List<LogProb>? = null,
    public val reasoningContent: List<LogProb>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LogProbs) return false
        return content == other.content && reasoningContent == other.reasoningContent
    }

    override fun hashCode(): Int {
        var result = content?.hashCode() ?: 0
        result = 31 * result + (reasoningContent?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "LogProbs(content=$content, reasoningContent=$reasoningContent)"
}

/**
 * Log-probability information for a single chat-completion token.
 *
 * @property token The token as text.
 * @property logprob Natural-log probability the model assigned to [token].
 * @property bytes UTF-8 byte values of [token], or `null` if the token is not byte-aligned.
 * @property topLogprobs Up to `topLogprobs` most likely alternatives considered at this
 * position, each with its own log-probability.
 */
@Serializable
public class LogProb(
    public val token: String,
    public val logprob: Double,
    public val bytes: List<Int>?,
    public val topLogprobs: List<TopLogProb>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LogProb) return false
        return token == other.token && logprob == other.logprob && bytes == other.bytes && topLogprobs == other.topLogprobs
    }

    override fun hashCode(): Int {
        var result = token.hashCode()
        result = 31 * result + logprob.hashCode()
        result = 31 * result + (bytes?.hashCode() ?: 0)
        result = 31 * result + topLogprobs.hashCode()
        return result
    }

    override fun toString(): String =
        "LogProb(token='$token', logprob=$logprob, bytes=$bytes, topLogprobs=$topLogprobs)"
}

/**
 * One of the top alternatives considered at a given token position in [LogProb.topLogprobs].
 *
 * @property token The alternative token as text.
 * @property logprob Natural-log probability the model assigned to [token].
 * @property bytes UTF-8 byte values of [token], or `null` if the token is not byte-aligned.
 */
@Serializable
public class TopLogProb(
    public val token: String,
    public val logprob: Double,
    public val bytes: List<Int>?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TopLogProb) return false
        return token == other.token && logprob == other.logprob && bytes == other.bytes
    }

    override fun hashCode(): Int {
        var result = token.hashCode()
        result = 31 * result + logprob.hashCode()
        result = 31 * result + (bytes?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "TopLogProb(token='$token', logprob=$logprob, bytes=$bytes)"
}

/**
 * Log-probability information attached to a [FIMChoice].
 *
 * Uses the legacy completions shape — aligned parallel lists over the generated tokens —
 * which differs from the per-token [LogProb] shape returned by chat endpoints.
 *
 * @property textOffset Character offset of each token into the generated text.
 * @property tokenLogprobs Natural-log probability of each token in [tokens].
 * @property tokens The generated tokens as text.
 * @property topLogprobs For each token position, the alternatives considered, or `null`
 * when the request did not ask for top-logprobs.
 */
@Serializable
public class FIMLogProbs(
    public val textOffset: List<Int>,
    public val tokenLogprobs: List<Double>,
    public val tokens: List<String>,
    public val topLogprobs: List<TopLogProb>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FIMLogProbs) return false
        return textOffset == other.textOffset && tokenLogprobs == other.tokenLogprobs && tokens == other.tokens && topLogprobs == other.topLogprobs
    }

    override fun hashCode(): Int {
        var result = textOffset.hashCode()
        result = 31 * result + tokenLogprobs.hashCode()
        result = 31 * result + tokens.hashCode()
        result = 31 * result + (topLogprobs?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FIMLogprobs(textOffset=$textOffset, tokenLogprobs=$tokenLogprobs, tokens=$tokens, topLogprobs=$topLogprobs)"
}