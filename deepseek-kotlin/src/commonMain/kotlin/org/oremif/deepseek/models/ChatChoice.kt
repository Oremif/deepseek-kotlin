package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * One completion alternative returned inside a [ChatCompletion].
 *
 * A [ChatCompletion] may contain several choices when the request asked the model to
 * produce multiple candidates; each choice has its own message and finish reason.
 *
 * @property finishReason Reason the model stopped generating tokens for this choice.
 * @property index Position of the choice in [ChatCompletion.choices].
 * @property message Assistant message produced by the model.
 * @property logprobs Log-probability information for the generated tokens, if requested
 * via the `logprobs` parameter.
 */
@Serializable
public class ChatChoice(
    public val finishReason: FinishReason,
    public val index: Long,
    public val message: ChatCompletionMessage,
    public val logprobs: LogProbs? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatChoice) return false

        return index == other.index &&
                finishReason == other.finishReason &&
                message == other.message &&
                logprobs == other.logprobs
    }

    override fun hashCode(): Int {
        var result = index.hashCode()
        result = 31 * result + finishReason.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + (logprobs?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ChatCompletionChoice(finishReason=$finishReason, index=$index, message=$message, logprobs=$logprobs)"
}