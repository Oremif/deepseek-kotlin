package org.oremif.deepseek.models

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Serializable(with = ToolFunctionSerializer::class)
public sealed interface ToolFunction {
    public val name: String
}

@Serializable
public class FunctionRequest(
    override val name: String,
    public val description: String?,
    public val parameters: JsonObject?,
) : ToolFunction {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionRequest) return false
        return name == other.name && description == other.description && parameters == other.parameters
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (parameters?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FunctionRequest(name='$name', description=$description, parameters=$parameters)"
}

@Serializable
public class FunctionResponse(
    override val name: String,
    public val arguments: JsonObject?,
) : ToolFunction {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionResponse) return false
        return name == other.name && arguments == other.arguments
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (arguments?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FunctionResponse(name='$name', arguments=$arguments)"
}

internal object ToolFunctionSerializer : JsonContentPolymorphicSerializer<ToolFunction>(ToolFunction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ToolFunction> {
        return when {
            "parameters" in element.jsonObject -> FunctionRequest.serializer()
            "arguments" in element.jsonObject -> FunctionResponse.serializer()
            else -> throw Exception("Unknown ToolFunction type")
        }
    }
}
