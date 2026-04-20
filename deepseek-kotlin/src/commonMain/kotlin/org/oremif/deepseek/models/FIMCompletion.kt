package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Response payload of a Fill-In-the-Middle completion.
 *
 * Streaming FIM endpoints emit a [kotlinx.coroutines.flow.Flow] of [FIMCompletion] chunks;
 * all chunks from the same response share [id] and [created]. For non-streaming calls the
 * single returned instance contains the full completion.
 *
 * @property id Unique identifier for the completion. Identical across all chunks of a
 * streamed response.
 * @property choices Completion alternatives generated for the input prompt.
 * @property created Unix timestamp (seconds) of when the completion was created.
 * @property model Model that produced the completion.
 * @property systemFingerprint Backend configuration fingerprint, if the API returned one.
 * @property object Object type discriminator; always `text_completion`.
 * @property usage Token usage statistics. Only populated on the final usage chunk when
 * `streamOptions.includeUsage` is set for streaming requests.
 */
@Serializable
public class FIMCompletion(
    public val id: String,
    public val choices: List<FIMChoice>,
    public val created: Long,
    public val model: String,
    public val systemFingerprint: String? = null,
    public val `object`: String,
    public val usage: Usage? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FIMCompletion) return false

        return id == other.id &&
                choices == other.choices &&
                created == other.created &&
                model == other.model &&
                systemFingerprint == other.systemFingerprint &&
                `object` == other.`object` &&
                usage == other.usage
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + choices.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + (systemFingerprint?.hashCode() ?: 0)
        result = 31 * result + `object`.hashCode()
        result = 31 * result + (usage?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FIMCompletion(id='$id', choices=$choices, created=$created, model='$model', systemFingerprint=$systemFingerprint, `object`='$`object`, usage=$usage)"
}