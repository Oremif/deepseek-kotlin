package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * One completion alternative returned inside a [FIMCompletion].
 *
 * For streamed FIM responses each emitted [FIMCompletion] typically carries a single
 * [FIMChoice] whose [text] contains only the tokens produced since the previous chunk.
 *
 * @property text Generated text for this choice. In streaming mode this contains only
 * the incremental tokens for the current chunk.
 * @property index Position of the choice in [FIMCompletion.choices].
 * @property finishReason Reason the model stopped generating tokens, or `null` while the
 * stream is still in progress.
 * @property logprobs Log-probability information for the generated tokens, if requested
 * via the `logprobs` parameter.
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