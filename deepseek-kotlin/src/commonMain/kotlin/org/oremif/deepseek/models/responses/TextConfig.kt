package org.oremif.deepseek.models.responses

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Shape of the text the model must produce.
 *
 * Structured output lives here and nowhere else in the DeepSeek API: `/chat/completions` only
 * offers free-form text and `json_object`, while [jsonSchema] additionally constrains the answer to
 * a schema of your own.
 *
 * Example:
 * ```kotlin
 * val params = responseParams {
 *     text = TextConfig.jsonSchema(
 *         name = "weather",
 *         schema = buildJsonObject {
 *             put("type", "object")
 *             putJsonObject("properties") { putJsonObject("city") { put("type", "string") } }
 *         },
 *     )
 * }
 * ```
 *
 * @property format The format the answer must take.
 */
@Serializable
public class TextConfig(public val format: TextFormat) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextConfig) return false
        return format == other.format
    }

    override fun hashCode(): Int = format.hashCode()

    override fun toString(): String = "TextConfig(format=$format)"

    public companion object {
        /** Free-form text. Equivalent to leaving `text` unset. */
        public val text: TextConfig = TextConfig(TextFormat.text)

        /**
         * Any valid JSON object. The prompt must also ask the model for JSON, exactly as with
         * [org.oremif.deepseek.models.ResponseFormat.jsonObject].
         */
        public val jsonObject: TextConfig = TextConfig(TextFormat.jsonObject)

        /**
         * JSON constrained to [schema].
         *
         * @param name Name of the schema, reported back to the model
         * @param schema JSON Schema the answer must conform to
         */
        public fun jsonSchema(name: String, schema: JsonObject): TextConfig =
            TextConfig(TextFormat.jsonSchema(name, schema))
    }
}

/**
 * Wire form of [TextConfig.format]. Build it through the [TextConfig] factories rather than
 * directly.
 *
 * @property type Format discriminator: `text`, `json_object` or `json_schema`.
 * @property name Name of the schema; set only when [type] is `json_schema`.
 * @property schema JSON Schema the answer must conform to; set only when [type] is `json_schema`.
 */
@Serializable
public class TextFormat
private constructor(
    public val type: String,
    public val name: String? = null,
    public val schema: JsonObject? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextFormat) return false
        return type == other.type && name == other.name && schema == other.schema
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (schema?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "TextFormat(type='$type', name=$name, schema=$schema)"

    public companion object {
        /** Free-form text response. */
        public val text: TextFormat = TextFormat("text")

        /** Response guaranteed to be a valid JSON object. */
        public val jsonObject: TextFormat = TextFormat("json_object")

        /**
         * Response constrained to a JSON Schema.
         *
         * @param name Name of the schema
         * @param schema JSON Schema the answer must conform to
         */
        public fun jsonSchema(name: String, schema: JsonObject): TextFormat =
            TextFormat("json_schema", name, schema)
    }
}
