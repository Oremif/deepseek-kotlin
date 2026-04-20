package org.oremif.deepseek.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Type discriminator for a tool that the model can call.
 *
 * The DeepSeek API currently only supports function-type tools; this enum exists to stay
 * aligned with the wire format and to accept future extensions.
 */
@Serializable
public enum class ToolCallType {
    /** A function tool described by [FunctionRequest]. Serializes to the string `function`. */
    @SerialName("function")
    FUNCTION,
}
