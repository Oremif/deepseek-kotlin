package org.oremif.deepseek.models

import kotlinx.serialization.SerialName

/**
 * Identifier of a DeepSeek chat model.
 *
 * Serialized to the string slug expected by the DeepSeek API (for example, `deepseek-chat`).
 *
 * @see <a href="https://api-docs.deepseek.com/quick_start/pricing">DeepSeek model list</a>
 */
public enum class ChatModel {
    /** General-purpose chat model. Corresponds to API identifier `deepseek-chat`. */
    @SerialName("deepseek-chat")
    DEEPSEEK_CHAT,

    /** Reasoning-optimised model that also returns reasoning traces. Corresponds to API identifier `deepseek-reasoner`. */
    @SerialName("deepseek-reasoner")
    DEEPSEEK_REASONER,
}
