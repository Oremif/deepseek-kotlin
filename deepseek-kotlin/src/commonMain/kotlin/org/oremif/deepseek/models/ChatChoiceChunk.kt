package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * One choice slice carried by a streamed [ChatCompletionChunk].
 *
 * Each chunk's choice delivers only the tokens produced since the previous chunk via
 * [delta]. [finishReason] is `null` until the stream terminates on this choice.
 *
 * @property delta Incremental message fragment produced on this step.
 * @property finishReason Reason the model stopped generating tokens for this choice, or
 * `null` if generation is still in progress.
 * @property index Position of the choice in [ChatCompletionChunk.choices].
 * @property logprobs Log-probability information for the tokens in [delta], if requested
 * via the `logprobs` parameter.
 */
@Serializable
public class ChatChoiceChunk(
    public val delta: ChatCompletionDelta,
    public val finishReason: FinishReason?,
    public val index: Long,
    public val logprobs: LogProbs? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatChoiceChunk) return false

        return delta == other.delta &&
                finishReason == other.finishReason &&
                index == other.index &&
                logprobs == other.logprobs
    }

    override fun hashCode(): Int {
        var result = delta.hashCode()
        result = 31 * result + (finishReason?.hashCode() ?: 0)
        result = 31 * result + index.hashCode()
        result = 31 * result + (logprobs?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ChatCompletionChoiceChunk(delta=$delta, finishReason=$finishReason, index=$index, logprobs=$logprobs)"
}