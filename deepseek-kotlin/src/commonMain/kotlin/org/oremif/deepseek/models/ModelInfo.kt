package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 *
 *
 * @property id The model identifier, which can be referenced in the API endpoints.
 * @property object **Possible values: {`model`}.** The object type, which is always "model".
 * @property ownedBy The organization that owns the model.
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