package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * @property finishReason The reason the model stopped generating tokens.
 * This will be `stop` if the model hit a natural stop point or a provided stop sequence,
 * `length` if the maximum number of tokens specified in the request was reached,
 * `content_filter` if content was omitted due to a flag from our content filters,
 * or `insufficient_system_resource` if the request is interrupted due to insufficient resource of the inference system.
 *
 * **Possible values: {`stop`, `length`, `content_filter`, `insufficient_system_resource`}**
 * @property index
 * @property logprobs
 * @property text
 */
@Serializable
public class FIMChoice(
    public val text: String,
    public val index: Int,
    public val finishReason: FinishReason? = null,
    public val logprobs: FIMLogProbs? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FIMChoice) return false
        return text == other.text && finishReason == other.finishReason && index == other.index && logprobs == other.logprobs
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (finishReason?.hashCode() ?: 0)
        result = 31 * result + index
        result = 31 * result + (logprobs?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FIMCompletionChoice(text='$text', finishReason=$finishReason, index=$index, logprobs=$logprobs)"
}