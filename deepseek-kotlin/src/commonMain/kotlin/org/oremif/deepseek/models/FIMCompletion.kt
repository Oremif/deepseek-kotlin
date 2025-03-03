package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Represents the FIM (Fill-In-the-Middle) Completion.
 *
 * @property id A unique identifier for the completion.
 * @property choices The list of completion choices the model generated for the input prompt.
 * @property created The Unix timestamp (in seconds) of when the completion was created.
 * @property model The model used for completion.
 * @property systemFingerprint This fingerprint represents the backend configuration that the model runs with.
 * @property object The object type, which is always "text_completion"
 *
 * **Possible values: {`text_completion`}**
 * @property usage Usage statistics for the completion request.
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