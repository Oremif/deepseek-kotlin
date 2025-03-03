package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
public class ListsModels(
    public val `object`: String,
    public val data: List<ModelInfo>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListsModels) return false
        return `object` == other.`object` && data == other.data
    }

    override fun hashCode(): Int {
        var result = `object`.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }

    override fun toString(): String =
        "ListsModels(`object`='$`object`', data=$data)"
}