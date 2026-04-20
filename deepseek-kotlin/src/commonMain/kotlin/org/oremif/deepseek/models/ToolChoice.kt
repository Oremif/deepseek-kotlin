package org.oremif.deepseek.models

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Controls how the model picks a tool to call.
 *
 * Implementations:
 * - [ChatCompletionToolChoice] — coarse strategies (`none`, `auto`, `required`).
 * - [ChatCompletionNamedToolChoice] — pin the model to a specific tool by name.
 *
 * When omitted, the DeepSeek API defaults to `auto` if tools are provided and `none`
 * otherwise.
 */
@Serializable(with = ToolChoiceSerializer::class)
public sealed interface ToolChoice

/**
 * Coarse tool-selection strategy.
 */
@Serializable
public enum class ChatCompletionToolChoice : ToolChoice {
    /** The model must not call any tool and must instead produce a message. */
    @SerialName("none")
    NONE,

    /** The model decides whether to produce a message or call tools. This is the default when tools are provided. */
    @SerialName("auto")
    AUTO,

    /** The model must call one or more of the provided tools. */
    @SerialName("required")
    REQUIRED
}

/**
 * Forces the model to call a specific tool.
 *
 * Construct via the JSON the DeepSeek API expects, e.g.
 * `{"type": "function", "function": {"name": "my_function"}}`.
 *
 * @property type Tool type discriminator; currently always [ToolCallType.FUNCTION].
 * @property function Reference to the tool that must be called; only the `name` field is
 * consumed by the API.
 */
@Serializable
public class ChatCompletionNamedToolChoice internal constructor(
    public val type: ToolCallType,
    public val function: ToolFunction,
) : ToolChoice {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatCompletionNamedToolChoice) return false
        return type == other.type && function == other.function
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + function.hashCode()
        return result
    }

    override fun toString(): String =
        "ChatCompletionNamedToolChoice(type=$type, function=$function)"
}


internal object ToolChoiceSerializer : JsonContentPolymorphicSerializer<ToolChoice>(ToolChoice::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ToolChoice> {
        return when {
            element is JsonObject && "type" in element.jsonObject -> ChatCompletionNamedToolChoice.serializer()
            else -> ChatCompletionToolChoice.serializer()
        }
    }
}
