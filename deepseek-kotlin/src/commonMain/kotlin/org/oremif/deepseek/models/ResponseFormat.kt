package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
public class ResponseFormat private constructor(public val type: String) {
    public companion object {
        public val text: ResponseFormat = ResponseFormat("text")
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