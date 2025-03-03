package org.oremif.deepseek.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class FinishReason {
    @SerialName("stop")
    STOP,
    @SerialName("length")
    LENGTH,
    @SerialName("content_filter")
    CONTENT_FILTER,
    @SerialName("tool_calls")
    TOOL_CALLS,
    @SerialName("insufficient_system_resource")
    INSUFFICIENT_SYSTEM_RESOURCE,
}