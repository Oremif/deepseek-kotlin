package org.oremif.deepseek.models

import kotlinx.serialization.Serializable

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