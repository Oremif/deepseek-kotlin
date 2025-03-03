package org.oremif.deepseek.models

import kotlinx.serialization.SerialName

public enum class ChatModel {
    @SerialName("deepseek-chat")
    DEEPSEEK_CHAT,

    @SerialName("deepseek-reasoner")
    DEEPSEEK_REASONER,
}