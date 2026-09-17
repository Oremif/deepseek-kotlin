package org.oremif.deepseek.models.responses

import kotlinx.serialization.Serializable
import org.oremif.deepseek.models.ReasoningEffort

/**
 * Thinking-mode configuration of a Responses API request.
 *
 * Unlike `/chat/completions`, which splits the toggle ([org.oremif.deepseek.models.Thinking]) from
 * the budget (`reasoning_effort`), the Responses API folds both into [effort]:
 * [ReasoningEffort.NONE] switches thinking off, and every other level switches it on with that
 * budget. Left unset, the model's own default applies — thinking on.
 *
 * Example:
 * ```kotlin
 * val quick = responseParams { reasoning = ReasoningConfig(ReasoningEffort.NONE) }
 * val deep = responseParams { reasoning = ReasoningConfig(ReasoningEffort.MAX) }
 * ```
 *
 * The API accepts `summary` as well but never generates one, so the SDK leaves it out.
 *
 * @property effort How much reasoning the model spends before answering.
 */
@Serializable
public class ReasoningConfig(public val effort: ReasoningEffort? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReasoningConfig) return false
        return effort == other.effort
    }

    override fun hashCode(): Int = effort?.hashCode() ?: 0

    override fun toString(): String = "ReasoningConfig(effort=$effort)"
}
