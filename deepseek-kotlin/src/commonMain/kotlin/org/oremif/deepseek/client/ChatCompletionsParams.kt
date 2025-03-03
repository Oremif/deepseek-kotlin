package org.oremif.deepseek.client

import org.oremif.deepseek.models.ChatModel
import org.oremif.deepseek.models.ResponseFormat
import org.oremif.deepseek.models.StopReason
import org.oremif.deepseek.models.Tool
import org.oremif.deepseek.models.ToolChoice
import kotlin.require

public fun chatCompletionParams(block: ChatCompletionParams.Builder.() -> Unit): ChatCompletionParams {
    return ChatCompletionParams.Builder().apply(block).build()
}

public class ChatCompletionParams(
    public val model: ChatModel,
    public val frequencyPenalty: Double? = null,
    public val maxTokens: Int? = null,
    public val presencePenalty: Double? = null,
    public val responseFormat: ResponseFormat? = null,
    public val stop: StopReason? = null,
    public val temperature: Double? = null,
    public val topP: Double? = null,
    public val tools: List<Tool>? = null,
    public val toolChoice: ToolChoice? = null,
    public val logprobs: Boolean? = null,
    public val topLogprobs: Int? = null,
) {
    public class Builder {
        public var model: ChatModel = ChatModel.DEEPSEEK_CHAT
        public var frequencyPenalty: Double? = null
        public var maxTokens: Int? = null
        public var presencePenalty: Double? = null
        public var responseFormat: ResponseFormat? = null
        public var stop: StopReason? = null
        public var temperature: Double? = null
        public var topP: Double? = null
        public var tools: List<Tool>? = null
        public var toolChoice: ToolChoice? = null
        public var logprobs: Boolean? = null
        public var topLogprobs: Int? = null

        internal fun build(): ChatCompletionParams {
            frequencyPenalty?.let { require(it in -2.0..2.0) { "frequencyPenalty must be between -2.0 and 2.0" } }
            maxTokens?.let { require(it in 1..8192) { "maxTokens must be between 1 and 8192" } }
            presencePenalty?.let { require(it in -2.0..2.0) { "presencePenalty must be between -2.0 and 2.0" } }
            temperature?.let { require(it in 0.0..2.0) { "temperature must be between 0.0 and 2.0" } }
            topLogprobs?.let { require(it <= 20) { "topLogprobs must be <= 20" } }

            return ChatCompletionParams(
                model = model,
                frequencyPenalty = frequencyPenalty,
                maxTokens = maxTokens,
                presencePenalty = presencePenalty,
                responseFormat = responseFormat,
                stop = stop,
                temperature = temperature,
                topP = topP,
                tools = tools,
                toolChoice = toolChoice,
                logprobs = logprobs,
                topLogprobs = topLogprobs,
            )
        }
    }
}