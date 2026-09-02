package org.oremif.deepseek.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Switches a model between its thinking and non-thinking mode.
 *
 * Omitting this field is equivalent to [ThinkingType.ENABLED]. A thinking response carries
 * its chain-of-thought in [ChatCompletionMessage.reasoningContent] and bills the extra
 * tokens under [CompletionTokenDetails.reasoningTokens]; the non-thinking mode answers
 * faster and cheaper. Either way [ChatCompletion.model] echoes the slug that was sent.
 *
 * How long that reasoning pass runs is a separate, top-level request field — see
 * [ReasoningEffort].
 *
 * Example:
 * ```kotlin
 * val params = chatCompletionParams {
 *     model = ChatModel.DEEPSEEK_V4_FLASH
 *     thinking = Thinking(ThinkingType.DISABLED) // skip the reasoning pass
 * }
 * ```
 *
 * @property type Whether the reasoning pass runs for this request.
 */
@Serializable
public class Thinking(
    public val type: ThinkingType,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Thinking) return false
        return type == other.type
    }

    override fun hashCode(): Int = type.hashCode()

    override fun toString(): String = "Thinking(type=$type)"
}

/**
 * Values accepted by [Thinking.type].
 */
@Serializable
public enum class ThinkingType {
    /** Reasoning pass is active; this is what an omitted [Thinking] means. */
    @SerialName("enabled")
    ENABLED,

    /**
     * Reasoning pass is suppressed: the model answers directly, without producing
     * `reasoning_content` — see [Thinking].
     */
    @SerialName("disabled")
    DISABLED,
}
