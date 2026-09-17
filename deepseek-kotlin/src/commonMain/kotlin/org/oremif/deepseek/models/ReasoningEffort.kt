package org.oremif.deepseek.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * How much reasoning a thinking model spends before it answers, serialized as the raw value the API
 * expects.
 *
 * Sent as the top-level `reasoning_effort` field, next to — not inside — [Thinking]. It only
 * matters while the reasoning pass runs: with [ThinkingType.DISABLED] the model answers directly
 * and the effort is moot. Leaving it unset asks for the API default, `high`.
 *
 * The same type carries the Responses API's `reasoning.effort`, which spans a wider scale — see
 * [org.oremif.deepseek.models.responses.ReasoningConfig]. There [NONE] switches thinking off
 * altogether, and the server folds the levels onto the same three budgets: [MINIMAL] and [LOW] onto
 * `low`, [MEDIUM] / [HIGH] / [XHIGH] onto `high`, and [MAX] onto `max`. On `/chat/completions` only
 * [LOW], [HIGH] and [MAX] are documented; `medium` and `xhigh` are accepted there and mapped onto
 * [HIGH], while [NONE] and [MINIMAL] belong to the Responses API — turn thinking off in a chat
 * request with [Thinking] instead.
 *
 * Any other value can be wrapped directly, so a level the SDK declares no constant for is still
 * usable:
 * ```kotlin
 * val params = chatCompletionParams {
 *     model = ChatModel.DEEPSEEK_V4_PRO
 *     reasoningEffort = ReasoningEffort.MAX
 * }
 * val future = chatCompletionParams { reasoningEffort = ReasoningEffort("ultra") }
 * ```
 *
 * @property value Raw effort level sent in the `reasoning_effort` field of a chat request, or in
 *   `reasoning.effort` of a Responses API request. Must not be blank.
 * @see <a href="https://api-docs.deepseek.com/api/create-chat-completion">Create Chat
 *   Completion</a>
 * @see <a href="https://api-docs.deepseek.com/api/create-response">Create Response</a>
 */
@Serializable
@JvmInline
public value class ReasoningEffort(public val value: String) {
    init {
        require(value.isNotBlank()) { "reasoning effort must not be blank" }
    }

    override fun toString(): String = value

    public companion object {
        /**
         * `none` — no reasoning pass at all. Responses API only; it is that API's way of spelling
         * [ThinkingType.DISABLED].
         */
        public val NONE: ReasoningEffort = ReasoningEffort("none")

        /** `minimal` — Responses API only; the server folds it onto [LOW]. */
        public val MINIMAL: ReasoningEffort = ReasoningEffort("minimal")

        /** `low` — the shortest reasoning pass: fastest and cheapest of the three budgets. */
        public val LOW: ReasoningEffort = ReasoningEffort("low")

        /** `medium` — accepted everywhere, folded onto [HIGH] by the server. */
        public val MEDIUM: ReasoningEffort = ReasoningEffort("medium")

        /** `high` — the level the API applies when no effort is requested. */
        public val HIGH: ReasoningEffort = ReasoningEffort("high")

        /** `xhigh` — accepted everywhere, folded onto [HIGH] by the server. */
        public val XHIGH: ReasoningEffort = ReasoningEffort("xhigh")

        /** `max` — the longest reasoning pass; slowest, and bills the most reasoning tokens. */
        public val MAX: ReasoningEffort = ReasoningEffort("max")
    }
}
