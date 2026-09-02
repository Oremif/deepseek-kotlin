package org.oremif.deepseek.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * How much reasoning a thinking model spends before it answers, serialized as the raw
 * value the API expects.
 *
 * Sent as the top-level `reasoning_effort` field, next to — not inside — [Thinking]. It
 * only matters while the reasoning pass runs: with [ThinkingType.DISABLED] the model
 * answers directly and the effort is moot. Leaving it unset asks for the API default,
 * `high`.
 *
 * The API also accepts `medium` and `xhigh`, which it maps onto [HIGH]; any other value
 * can be wrapped directly, so a level the SDK declares no constant for is still usable:
 * ```kotlin
 * val params = chatCompletionParams {
 *     model = ChatModel.DEEPSEEK_V4_PRO
 *     reasoningEffort = ReasoningEffort.MAX
 * }
 * val compat = chatCompletionParams { reasoningEffort = ReasoningEffort("xhigh") }
 * ```
 *
 * @property value Raw effort level sent in the `reasoning_effort` field of a request.
 * Must not be blank.
 * @see <a href="https://api-docs.deepseek.com/api/create-chat-completion">Create Chat Completion</a>
 */
@Serializable
@JvmInline
public value class ReasoningEffort(public val value: String) {
    init {
        require(value.isNotBlank()) { "reasoning effort must not be blank" }
    }

    override fun toString(): String = value

    public companion object {
        /** `low` — the shortest reasoning pass: fastest and cheapest of the three. */
        public val LOW: ReasoningEffort = ReasoningEffort("low")

        /** `high` — the level the API applies when `reasoning_effort` is omitted. */
        public val HIGH: ReasoningEffort = ReasoningEffort("high")

        /** `max` — the longest reasoning pass; slowest, and bills the most reasoning tokens. */
        public val MAX: ReasoningEffort = ReasoningEffort("max")
    }
}
