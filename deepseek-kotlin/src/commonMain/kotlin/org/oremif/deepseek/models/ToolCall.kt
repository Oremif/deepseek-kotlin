package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Declaration of a tool the model may call during a chat completion.
 *
 * The DeepSeek API currently supports function tools only; provide the schema via
 * [FunctionRequest.parameters] so the model can emit matching arguments.
 *
 * Example:
 * ```kotlin
 * val weather = Tool(
 *     type = ToolCallType.FUNCTION,
 *     function = FunctionRequest(
 *         name = "get_weather",
 *         description = "Get the current weather for a city",
 *         parameters = buildJsonObject {
 *             put("type", "object")
 *             putJsonObject("properties") {
 *                 putJsonObject("city") { put("type", "string") }
 *             }
 *             putJsonArray("required") { add("city") }
 *         },
 *     ),
 * )
 * ```
 *
 * @property type Tool type discriminator; currently always [ToolCallType.FUNCTION].
 * @property function Function signature the model can call.
 */
@Serializable
public class Tool(
    public val type: ToolCallType,
    public val function: FunctionRequest,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tool) return false
        return type == other.type && function == other.function
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + function.hashCode()
        return result
    }

    override fun toString(): String =
        "Tool(type=$type, function=$function)"
}

/**
 * A tool invocation emitted by the model inside a [ChatCompletionMessage].
 *
 * Echo [id] back in a follow-up [ToolMessage] so the model can correlate the tool result
 * with its original call.
 *
 * @property id Identifier that must be echoed back in the matching [ToolMessage].
 * @property type Tool type discriminator; currently always [ToolCallType.FUNCTION].
 * @property function Function name the model asked to call, together with the JSON
 * arguments it produced.
 */
@Serializable
public class ToolCall(
    public val id: String,
    public val type: ToolCallType,
    public val function: FunctionResponse,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ToolCall) return false
        return id == other.id && type == other.type && function == other.function
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + function.hashCode()
        return result
    }

    override fun toString(): String =
        "ToolCall(id='$id', type=$type, function=$function)"
}