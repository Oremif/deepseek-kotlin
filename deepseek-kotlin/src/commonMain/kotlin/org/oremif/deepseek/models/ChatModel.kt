package org.oremif.deepseek.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Identifier of a DeepSeek model, serialized as the raw slug the API expects.
 *
 * Any slug can be wrapped directly, so a model the SDK declares no constant for is still
 * usable:
 * ```kotlin
 * val params = chatCompletionParams { model = ChatModel.DEEPSEEK_V4_PRO }
 * val preview = chatCompletionParams { model = ChatModel("deepseek-v4-preview") }
 * ```
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
         * `deepseek-v4-flash` — general-purpose model with a 1M token context and up to
         * 384K output tokens. Thinks unless [Thinking] disables it.
         */
        public val DEEPSEEK_V4_FLASH: ChatModel = ChatModel("deepseek-v4-flash")

        /**
         * `deepseek-v4-pro` — same limits and features as [DEEPSEEK_V4_FLASH], with the
         * strongest capability and a lower concurrency limit. The only model the FIM
         * endpoint accepts.
         */
        public val DEEPSEEK_V4_PRO: ChatModel = ChatModel("deepseek-v4-pro")

        /**
         * `deepseek-v4-flash-vision-exp` — [DEEPSEEK_V4_FLASH] plus image content parts in
         * user messages. Rejected by the FIM endpoint.
         */
        public val DEEPSEEK_V4_FLASH_VISION_EXP: ChatModel = ChatModel("deepseek-v4-flash-vision-exp")

        /** `deepseek-chat` — no longer served; requests naming it fail. */
        @Deprecated(
            "The DeepSeek API no longer serves this model. Use DEEPSEEK_V4_FLASH.",
            ReplaceWith("ChatModel.DEEPSEEK_V4_FLASH"),
            DeprecationLevel.ERROR,
        )
        public val DEEPSEEK_CHAT: ChatModel = ChatModel("deepseek-chat")

        /** `deepseek-reasoner` — no longer served; requests naming it fail. */
        @Deprecated(
            "The DeepSeek API no longer serves this model. Use DEEPSEEK_V4_FLASH.",
            ReplaceWith("ChatModel.DEEPSEEK_V4_FLASH"),
            DeprecationLevel.ERROR,
        )
        public val DEEPSEEK_REASONER: ChatModel = ChatModel("deepseek-reasoner")
    }
}
