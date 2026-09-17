package org.oremif.deepseek.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Identifier of a DeepSeek model, serialized as the raw slug the API expects.
 *
 * The API serves two models — [DEEPSEEK_FLASH] and [DEEPSEEK_V4_PRO] — and answers an unknown slug
 * with a 400 that names them both, so a typo fails loudly rather than silently picking a model:
 * ```
 * The supported API model names are deepseek-flash, deepseek-v4-pro,
 * but you passed deepseek-does-not-exist.
 * ```
 *
 * Several retired names still resolve to [DEEPSEEK_FLASH] — `deepseek-chat`, `deepseek-reasoner`,
 * `deepseek-v4-flash` and `deepseek-v4-flash-vision-exp`. The SDK declares constants only for the
 * two the API acknowledges; any other slug can be wrapped directly, so an alias, or a model
 * released after this version, stays usable:
 * ```kotlin
 * val params = chatCompletionParams { model = ChatModel.DEEPSEEK_V4_PRO }
 * val alias = chatCompletionParams { model = ChatModel("deepseek-v4-flash") }
 * ```
 *
 * A response echoes the model that actually ran, which for every alias is `deepseek-flash` — read
 * [ChatCompletion.model] rather than assuming the slug that was sent.
 *
 * @property id Raw model slug sent in the `model` field of a request. Must not be blank.
 * @see <a href="https://api-docs.deepseek.com/quick_start/pricing">DeepSeek model list</a>
 */
@Serializable
@JvmInline
public value class ChatModel(public val id: String) {
    init {
        require(id.isNotBlank()) { "model id must not be blank" }
    }

    override fun toString(): String = id

    public companion object {
        /**
         * `deepseek-flash` — the general-purpose model: a 1M token context, up to 384K output
         * tokens, and **image input**, which needs no separate vision model. Thinks unless
         * [Thinking] disables it.
         */
        public val DEEPSEEK_FLASH: ChatModel = ChatModel("deepseek-flash")

        /**
         * `deepseek-v4-pro` — the same limits and features as [DEEPSEEK_FLASH], with the strongest
         * capability and a lower concurrency limit.
         */
        public val DEEPSEEK_V4_PRO: ChatModel = ChatModel("deepseek-v4-pro")

        /** `deepseek-chat` — a retired name the API still resolves to [DEEPSEEK_FLASH]. */
        @Deprecated(
            "Retired name, still served as an alias of deepseek-flash. Use DEEPSEEK_FLASH.",
            ReplaceWith("ChatModel.DEEPSEEK_FLASH"),
            DeprecationLevel.WARNING,
        )
        public val DEEPSEEK_CHAT: ChatModel = ChatModel("deepseek-chat")

        /** `deepseek-reasoner` — a retired name the API still resolves to [DEEPSEEK_FLASH]. */
        @Deprecated(
            "Retired name, still served as an alias of deepseek-flash. Use DEEPSEEK_FLASH.",
            ReplaceWith("ChatModel.DEEPSEEK_FLASH"),
            DeprecationLevel.WARNING,
        )
        public val DEEPSEEK_REASONER: ChatModel = ChatModel("deepseek-reasoner")
    }
}
