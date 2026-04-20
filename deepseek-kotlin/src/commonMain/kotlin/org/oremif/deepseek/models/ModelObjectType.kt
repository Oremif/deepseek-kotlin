package org.oremif.deepseek.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Discriminator value returned in the `object` field of a [ModelInfo] entry.
 *
 * Present primarily for API compatibility; currently only the [MODEL] variant is defined.
 */
@Serializable
public enum class ModelObjectType {
    /** The entry describes a DeepSeek model. Serializes to the string `model`. */
    @SerialName("model")
    MODEL,
}
