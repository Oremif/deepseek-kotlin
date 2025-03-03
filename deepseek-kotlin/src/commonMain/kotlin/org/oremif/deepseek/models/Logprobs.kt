package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
public class LogProbs(
    public val content: List<LogProb>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LogProbs) return false
        return content == other.content
    }

    override fun hashCode(): Int {
        return content?.hashCode() ?: 0
    }

    override fun toString(): String =
        "LogProbs(content=$content)"
}

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
        result = 31 * result + topLogprobs.hashCode()
        return result
    }

    override fun toString(): String =
        "FIMLogprobs(textOffset=$textOffset, tokenLogprobs=$tokenLogprobs, tokens=$tokens, topLogprobs=$topLogprobs)"
}