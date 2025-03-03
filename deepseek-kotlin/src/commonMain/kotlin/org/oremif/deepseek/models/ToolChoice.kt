package org.oremif.deepseek.models

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Serializable(with = ToolChoiceSerializer::class)
public sealed interface ToolChoice

@Serializable
public enum class ChatCompletionToolChoice : ToolChoice {
    @SerialName("none")
    NONE,

    @SerialName("auto")
    AUTO,

    @SerialName("required")
    REQUIRED
}

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
