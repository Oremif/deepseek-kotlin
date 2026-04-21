package org.oremif.deepseek.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Toggles the reasoning ("thinking") pass of the `deepseek-reasoner` model.
 *
 * When omitted, `deepseek-reasoner` behaves as if [ThinkingType.ENABLED] were passed.
 *
 * Important behaviour observed against the live API: sending
 * `Thinking(ThinkingType.DISABLED)` together with `model = [ChatModel.DEEPSEEK_REASONER]`
 * causes the server to route the request to `deepseek-chat`. The returned
 * [ChatCompletion.model] comes back as `"deepseek-chat"` and no `reasoning_content` is
 * produced. Consumers that branch on `response.model` must account for this rewrite.
 *
 * Example:
 * ```kotlin
 * val params = chatCompletionParams {
 *     model = ChatModel.DEEPSEEK_REASONER
 *     thinking = Thinking(ThinkingType.DISABLED) // server routes to deepseek-chat
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
    /** Reasoning pass is active (default behaviour of `deepseek-reasoner`). */
    @SerialName("enabled")
    ENABLED,

    /**
     * Reasoning pass is suppressed. Paired with `model = deepseek-reasoner`, the server
     * transparently downgrades the request to `deepseek-chat` — see [Thinking].
     */
    @SerialName("disabled")
    DISABLED,
}
