package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * @property id A unique identifier for the chat completion. Each chunk has the same ID.
 * @property choices A list of chat completion choices.
 * @property created The Unix timestamp (in seconds) of when the chat completion was created.
 * Each chunk has the same timestamp.
 * @property model The model to generate the completion.
 * @property systemFingerprint This fingerprint represents the backend configuration that the model runs with.
 * @property object The object type, which is always `chat.completion.chunk`.
 *
 * **Possible values: {`chat.completion.chunk`}**
 */
@Serializable
public class ChatCompletionChunk(
    public val id: String,
    public val choices: List<ChoiceChunk>,
    public val created: Long,
    public val model: String,
    public val systemFingerprint: String,
    public val `object`: String,
    public val usage: Usage?,
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
        result = 31 * result + systemFingerprint.hashCode()
        result = 31 * result + `object`.hashCode()
        result = 31 * result + (usage?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ChatCompletionChunk(id='$id', choices=$choices, created=$created, model='$model', systemFingerprint='$systemFingerprint', `object`='$`object`', usage=$usage)"
}