package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

/**
 * Format the model must use for its response.
 *
 * Use [jsonObject] to request a response guaranteed to be a valid JSON object; use [text]
 * for the default free-form output.
 *
 * Example:
 * ```kotlin
 * val params = chatCompletionParams {
 *     responseFormat = ResponseFormat.jsonObject
 * }
 * ```
 *
 * @property type Wire value sent to the API; either `text` or `json_object`.
 */
@Serializable
public class ResponseFormat private constructor(public val type: String) {
    public companion object {
        /** Free-form text response. Equivalent to not setting `responseFormat` at all. */
        public val text: ResponseFormat = ResponseFormat("text")

        /**
         * Response guaranteed to be a valid JSON object. The request must also instruct the
         * model to produce JSON (e.g. via a system prompt).
         */
        public val jsonObject: ResponseFormat = ResponseFormat("json_object")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResponseFormat) return false
        return type == other.type
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String = "ResponseFormat(type='$type')"
}