package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * One chunk of a streamed chat completion response.
 *
 * Streaming chat endpoints emit a [kotlinx.coroutines.flow.Flow] of these chunks; all
 * chunks from the same response share [id] and [created]. Clients typically concatenate
 * [ChatCompletionDelta.content] across chunks to reconstruct the full assistant message.
 *
 * @property id Unique identifier shared by every chunk of a single response.
 * @property choices List of incremental choice updates produced on this step.
 * @property created Unix timestamp (seconds) of the response; identical across chunks.
 * @property model Model that produced the response.
 * @property systemFingerprint Backend configuration fingerprint, if the API returned one.
 * @property object Object type discriminator; always `chat.completion.chunk`.
 * @property usage Token usage statistics. Only populated on the final usage chunk when
 * `streamOptions.includeUsage` is set; `null` on regular content chunks.
 */
@Serializable
public class ChatCompletionChunk(
    public val id: String,
    public val choices: List<ChatChoiceChunk>,
    public val created: Long,
    public val model: String,
    public val systemFingerprint: String? = null,
    public val `object`: String,
    public val usage: Usage? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatCompletionChunk) return false

        return id == other.id &&
                choices == other.choices &&
                created == other.created &&
                model == other.model &&
                systemFingerprint == other.systemFingerprint &&
                `object` == other.`object` &&
                usage == other.usage
    }

    override fun hashCode(): Int {
        var result = created.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + choices.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + (systemFingerprint?.hashCode() ?: 0)
        result = 31 * result + `object`.hashCode()
        result = 31 * result + (usage?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ChatCompletionChunk(id='$id', choices=$choices, created=$created, model='$model', systemFingerprint='$systemFingerprint', `object`='$`object`', usage=$usage)"
}