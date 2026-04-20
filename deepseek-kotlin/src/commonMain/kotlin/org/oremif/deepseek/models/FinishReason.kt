package org.oremif.deepseek.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Reason the model stopped producing tokens for a [ChatChoice] or [FIMChoice].
 */
@Serializable
public enum class FinishReason {
    /** The model hit a natural stop point or a provided stop sequence. */
    @SerialName("stop")
    STOP,

    /** The maximum number of tokens specified in the request was reached. */
    @SerialName("length")
    LENGTH,

    /** Content was omitted due to a content-filter flag. */
    @SerialName("content_filter")
    CONTENT_FILTER,

    /** The model called one or more tools. Chat completions only. */
    @SerialName("tool_calls")
    TOOL_CALLS,

    /** The request was interrupted because the inference system ran out of resources. */
    @SerialName("insufficient_system_resource")
    INSUFFICIENT_SYSTEM_RESOURCE,
}
