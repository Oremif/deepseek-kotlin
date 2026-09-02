package org.oremif.deepseek.models

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * A function referenced by a tool.
 *
 * Has two concrete forms: [FunctionRequest] — the signature the model may call, sent in
 * the request — and [FunctionResponse] — the call the model actually emitted, received in
 * the response. The [ToolFunctionSerializer] picks the right form at deserialization
 * based on the presence of `parameters` vs. `arguments`.
 *
 * @property name Function name, shared by both variants.
 */
@Serializable(with = ToolFunctionSerializer::class)
public sealed interface ToolFunction {
    public val name: String
}

/**
 * Function signature declared in a chat completion request.
 *
 * @property name Function name the model can call.
 * @property description Natural-language description of what the function does; helps the
 * model decide when to call it.
 * @property parameters JSON-Schema description of the arguments the function accepts.
 * @property strict Whether the API constrains the generated arguments to
 * [parameters] instead of merely prompting for them. Beta feature; left out of the
 * request when `null`, which the API reads as `false`.
 */
@Serializable
public class FunctionRequest(
    override val name: String,
    public val description: String?,
    public val parameters: JsonObject?,
    public val strict: Boolean? = null,
) : ToolFunction {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionRequest) return false
        return name == other.name &&
                description == other.description &&
                parameters == other.parameters &&
                strict == other.strict
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (parameters?.hashCode() ?: 0)
        result = 31 * result + (strict?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FunctionRequest(name='$name', description=$description, parameters=$parameters, strict=$strict)"
}

/**
 * Function invocation emitted by the model inside a [ToolCall].
 *
 * @property name Name of the function the model asked to call.
 * @property arguments Arguments the model generated, as a raw JSON string. Not guaranteed
 * to be valid JSON, nor to match the schema declared in the corresponding
 * [FunctionRequest.parameters]; [argumentsAsJsonOrNull] parses it defensively.
 */
@Serializable
public class FunctionResponse(
    override val name: String,
    public val arguments: String?,
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

/**
 * Parses [FunctionResponse.arguments] into a [JsonObject], or returns `null` when the
 * model produced no arguments, malformed JSON, or a JSON value that is not an object.
 *
 * Example:
 * ```kotlin
 * val call = response.choices.first().message.toolCalls?.first()
 * val city = call?.function?.argumentsAsJsonOrNull()
 *     ?.get("city")?.jsonPrimitive?.content
 * ```
 *
 * @param json [Json] instance used for parsing; the default is sufficient for plain
 * argument objects.
 * @return The parsed arguments, or `null` if they are absent or not a valid JSON object.
 */
public fun FunctionResponse.argumentsAsJsonOrNull(json: Json = Json): JsonObject? =
    arguments?.let { runCatching { json.parseToJsonElement(it) as? JsonObject }.getOrNull() }

internal object ToolFunctionSerializer : JsonContentPolymorphicSerializer<ToolFunction>(ToolFunction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ToolFunction> {
        val obj = element as? JsonObject
            ?: throw SerializationException("Expected JsonObject for ToolFunction, got: ${element::class.simpleName}")
        return when {
            "parameters" in obj -> FunctionRequest.serializer()
            "arguments" in obj -> FunctionResponse.serializer()
            else -> throw SerializationException("Unknown ToolFunction variant: keys=${obj.keys}")
        }
    }
}
