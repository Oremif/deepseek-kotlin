package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Metadata for a single DeepSeek model entry returned by [org.oremif.deepseek.api.models].
 *
 * @property id Model identifier that can be passed to chat or FIM endpoints (e.g.
 * `deepseek-chat`).
 * @property object Object type discriminator; always [ModelObjectType.MODEL].
 * @property ownedBy Organization that owns the model.
 */
@Serializable
public class ModelInfo(
    public val id: String,
    public val `object`: ModelObjectType,
    public val ownedBy: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelInfo) return false
        return id == other.id && `object` == other.`object` && ownedBy == other.ownedBy
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + `object`.hashCode()
        result = 31 * result + ownedBy.hashCode()
        return result
    }

    override fun toString(): String =
        "Model(id='$id', `object`=$`object`, ownedBy='$ownedBy')"
}