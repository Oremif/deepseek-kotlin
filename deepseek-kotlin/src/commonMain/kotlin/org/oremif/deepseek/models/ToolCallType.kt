package org.oremif.deepseek.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class ToolCallType {
    @SerialName("function")
   FUNCTION,
}